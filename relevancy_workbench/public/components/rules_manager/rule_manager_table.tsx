import {
  CREATE_REWRITER_MESSAGE,
  pageStyles,
  PLUGIN_RULES_MANAGER_NAME,
  QUERQY_DOCUMENTATION_LINK,
} from '../../../common';
import React, { ReactChild, ReactElement, useEffect, useState } from 'react';
import { ChromeBreadcrumb, CoreStart } from '../../../../../src/core/public';
import {
  EuiButton,
  EuiContextMenuItem,
  EuiContextMenuPanel,
  EuiFlexGroup,
  EuiFlexItem,
  EuiHorizontalRule,
  EuiLink,
  EuiOverlayMask,
  EuiPage,
  EuiPageBody,
  EuiPageContent,
  EuiPageContentHeader,
  EuiPageContentHeaderSection,
  EuiPageHeader,
  EuiPageHeaderSection,
  EuiPopover,
  EuiSpacer,
  EuiText,
  EuiTitle,
} from '@elastic/eui';
import _ from 'lodash';
import { DeleteObjectModal, getCustomModal } from '../common_utils/modals/modal_containers';
import { TableModule } from './table_module';

interface RuleManagerTableProps {
  parentBreadCrumbs: ChromeBreadcrumb[];
  notifications: CoreStart['notifications'];
  http: CoreStart['http'];
  setBreadcrumbs: (newBreadcrumbs: ChromeBreadcrumb[]) => void;
  fetchAllRewriters: () => void;
  rewriterList: any[];
  tableLoading: boolean;
  renameRewriter: (currentRewriterId: string, newRewriterId: string) => Promise<void>;
  deleteRewriter: (rewriterId: string, showToast: boolean) => Promise<any>;
  setToast: (title: string, color?: string, text?: ReactChild, side?: string) => void;
  duplicateRewriter: (currentRewriterId: string, newRewriterId: string) => Promise<void>;
  createRewriter: (newRewriterId: string, newRewriterType: string) => Promise<void> | undefined;
}

export const RuleManagerTable = ({
  parentBreadCrumbs,
  notifications,
  http,
  setBreadcrumbs,
  fetchAllRewriters,
  rewriterList,
  tableLoading,
  renameRewriter,
  deleteRewriter,
  setToast,
  duplicateRewriter,
  createRewriter,
}: RuleManagerTableProps) => {
  const [isModalVisible, setIsModalVisible] = useState(false); // Modal Toggle
  const [modalLayout, setModalLayout] = useState(<EuiOverlayMask />); // Modal Layout
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRewriters, setSelectedRewriters] = useState<any[]>([]);
  const [isActionsPopoverOpen, setIsActionsPopoverOpen] = useState(false);

  const onCreate = async (newRewriterId: string, rewriterType: string) => {
    createRewriter(newRewriterId, rewriterType);
    closeModal();
  };

  const createNewRewiter = () => {
    setModalLayout(
      getCustomModal(
        onCreate,
        closeModal,
        'Name',
        'Create rewriter',
        'Cancel',
        'Create',
        undefined,
        CREATE_REWRITER_MESSAGE,
        undefined,
        true
      )
    );
    showModal();
  };

  const onRename = async (newRewriterId: string) => {
    renameRewriter(selectedRewriters[0].rewriterId, newRewriterId);
    closeModal();
  };

  const onClone = async (newRewriterId: string) => {
    duplicateRewriter(selectedRewriters[0].rewriterId, newRewriterId);
    closeModal();
  };

  const onDelete = async () => {
    const toastMessage = `Rewriter${
      selectedRewriters.length > 1 ? 's' : ' ' + selectedRewriters[0].rewriterId
    } successfully deleted!`;
    Promise.all(selectedRewriters.map((rewriter) => deleteRewriter(rewriter.rewriterId, false)))
      .then(() => setToast(toastMessage))
      .catch((err) => {
        setToast(
          'Error deleting rewriters, please make sure you have the correct permission.',
          'danger'
        );
        console.error(err.body.message);
      });
    closeModal();
  };

  const deletePanel = () => {
    const rewriterString = `rewriter${selectedRewriters.length > 1 ? 's' : ''}`;
    setModalLayout(
      <DeleteObjectModal
        onConfirm={onDelete}
        onCancel={closeModal}
        title={`Delete ${selectedRewriters.length} ${rewriterString}`}
        message={`Are you sure you want to delete the selected ${selectedRewriters.length} ${rewriterString}?`}
      />
    );
    showModal();
  };
  const renameRewriterById = () => {
    setModalLayout(
      getCustomModal(
        onRename,
        closeModal,
        'Name',
        'Rename Rewriter',
        'Cancel',
        'Rename',
        selectedRewriters[0].rewriterId,
        CREATE_REWRITER_MESSAGE
      )
    );
    showModal();
  };

  const cloneRewriterById = () => {
    setModalLayout(
      getCustomModal(
        onClone,
        closeModal,
        'Name',
        'Duplicate Rewriter',
        'Cancel',
        'Duplicate',
        selectedRewriters[0].rewriterId + ' (copy)',
        CREATE_REWRITER_MESSAGE
      )
    );
    showModal();
  };

  const closeModal = () => {
    setIsModalVisible(false);
  };

  const showModal = () => {
    setIsModalVisible(true);
  };

  const popoverButton = (
    <EuiButton
      data-test-subj="rewritersActionsButton"
      iconType="arrowDown"
      iconSide="right"
      onClick={() => setIsActionsPopoverOpen(!isActionsPopoverOpen)}
    >
      Actions
    </EuiButton>
  );

  const popoverItems: ReactElement[] = [
    <EuiContextMenuItem
      key="rename"
      disabled={rewriterList.length === 0 || selectedRewriters.length !== 1}
      onClick={() => {
        setIsActionsPopoverOpen(false);
        renameRewriterById();
      }}
    >
      Rename
    </EuiContextMenuItem>,
    <EuiContextMenuItem
      key="duplicate"
      disabled={rewriterList.length === 0 || selectedRewriters.length !== 1}
      onClick={() => {
        setIsActionsPopoverOpen(false);
        cloneRewriterById();
      }}
    >
      Duplicate
    </EuiContextMenuItem>,
    <EuiContextMenuItem
      key="delete"
      data-test-subj="deleteContextMenuItem"
      disabled={rewriterList.length === 0 || selectedRewriters.length === 0}
      onClick={() => {
        setIsActionsPopoverOpen(false);
        deletePanel();
      }}
    >
      Delete
    </EuiContextMenuItem>,
  ];

  useEffect(() => {
    setBreadcrumbs([...parentBreadCrumbs]);
    fetchAllRewriters();
  }, []);

  return (
    <div style={pageStyles}>
      <EuiPage>
        <EuiPageBody component="div">
          <EuiPageHeader>
            <EuiPageHeaderSection>
              <EuiTitle size="l">
                <h1>{PLUGIN_RULES_MANAGER_NAME}</h1>
              </EuiTitle>
            </EuiPageHeaderSection>
          </EuiPageHeader>
          <EuiPageContent id="rewriterArea">
            <EuiPageContentHeader>
              <EuiPageContentHeaderSection>
                <EuiTitle size="s">
                  <h3>
                    Rewriters
                    <span className="rewriter-header-count"> ({rewriterList.length})</span>
                  </h3>
                </EuiTitle>
                <EuiSpacer size="s" />
                <EuiText size="s" color="subdued">
                  Use rules manager to create, edit and publish different querqy based rewriter
                  rules.{' '}
                  <EuiLink external={true} href={QUERQY_DOCUMENTATION_LINK} target="blank">
                    Learn more
                  </EuiLink>
                </EuiText>
              </EuiPageContentHeaderSection>
              <EuiPageContentHeaderSection>
                <EuiFlexGroup gutterSize="s">
                  <EuiFlexItem>
                    <EuiPopover
                      panelPaddingSize="none"
                      button={popoverButton}
                      isOpen={isActionsPopoverOpen}
                      closePopover={() => setIsActionsPopoverOpen(false)}
                    >
                      <EuiContextMenuPanel items={popoverItems} />
                    </EuiPopover>
                  </EuiFlexItem>
                  <EuiFlexItem>
                    <EuiButton fill onClick={createNewRewiter} data-test-subj="rewriter_createNew">
                      Create Rewriter
                    </EuiButton>
                  </EuiFlexItem>
                </EuiFlexGroup>
              </EuiPageContentHeaderSection>
            </EuiPageContentHeader>
            <EuiHorizontalRule margin="m" />
            <TableModule
              parentBreadCrumbs={parentBreadCrumbs}
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              setSelectedRewriters={setSelectedRewriters}
              rewriterList={rewriterList}
              tableLoading={tableLoading}
            />
          </EuiPageContent>
        </EuiPageBody>
      </EuiPage>
      {isModalVisible && modalLayout}
    </div>
  );
};
