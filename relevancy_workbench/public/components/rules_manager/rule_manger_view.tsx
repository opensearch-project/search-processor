import { ChromeBreadcrumb, CoreStart } from '../../../../../src/core/public';
import React, { ReactChild, useEffect, useState } from 'react';
import {
  BASE_RELEVENCY_WORKBENCH_API,
  CREATE_REWRITER_MESSAGE,
  PLUGIN_RULES_MANAGER_URL,
  REWRITER_TYPE_MAP,
} from '../../../common';
import {
  EuiButton,
  EuiContextMenu,
  EuiContextMenuPanelDescriptor,
  EuiFlexGroup,
  EuiFlexItem,
  EuiOverlayMask,
  EuiPage,
  EuiPageBody,
  EuiPageContentBody,
  EuiPageHeader,
  EuiPageHeaderSection,
  EuiPopover,
  EuiSpacer,
  EuiTitle,
} from '@elastic/eui';
import { DeleteObjectModal, getCustomModal } from '../common_utils/modals/modal_containers';
import _ from 'lodash';
import { CommonRulesView } from './view_module';

interface RuleManagerViewProps {
  rewriterId: string;
  parentBreadCrumbs: ChromeBreadcrumb[];
  http: CoreStart['http'];
  setBreadcrumbs: (newBreadcrumbs: ChromeBreadcrumb[]) => void;
  renameRewriter: (currentRewriterId: string, newRewriterId: string) => Promise<void>;
  deleteRewriter: (rewriterId: string, showToast: boolean) => Promise<any>;
  setToast: (title: string, color?: string, text?: ReactChild, side?: string) => void;
  duplicateRewriter: (currentRewriterId: string, newRewriterId: string) => Promise<void>;
  createRewriter: (newRewriterId: string, newRewriterType: string) => Promise<void> | undefined;
}

export const RuleManagerView = ({
  rewriterId,
  parentBreadCrumbs,
  http,
  setBreadcrumbs,
  renameRewriter,
  deleteRewriter,
  setToast,
  duplicateRewriter,
  createRewriter,
}: RuleManagerViewProps) => {
  const [rewriterContent, setRewriterContent] = useState({} as any);
  const [contentLoading, setContentLoading] = useState(false);
  const [rewriterMenuPopover, setRewriterMenuPopover] = useState(false);
  const [isModalVisible, setIsModalVisible] = useState(false); // Modal Toggle
  const [modalLayout, setModalLayout] = useState(<EuiOverlayMask />); // Modal Layout
  const [configBody, setconfigBody] = useState({});

  const closeModal = () => {
    setIsModalVisible(false);
  };

  const showModal = () => {
    setIsModalVisible(true);
  };

  const ruleManagerViewBreadCrumb = [
    ...parentBreadCrumbs,
    {
      text: `${rewriterId}`,
      href: `#${PLUGIN_RULES_MANAGER_URL}/${rewriterId}`,
    },
  ];

  const onSave = () => {
    setContentLoading(true);
    http
      .put(BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER, {
        body: JSON.stringify({
          rewriterId: rewriterId,
          rewriterType: rewriterContent.class,
          config: configBody,
        }),
      })
      .then((res) => {
        setToast('Rewriter Saved');
      })
      .catch((error: Error) => {
        console.error(error);
      })
      .finally(() => {
        setContentLoading(false);
      });
  };

  const fetchRewriterById = () => {
    setContentLoading(true);
    http
      .get(`${BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER}/${rewriterId}`)
      .then((res) => {
        console.log(res);
        setRewriterContent(res);
      })
      .catch((error: Error) => {
        console.error(error);
      })
      .finally(() => {
        setContentLoading(false);
      });
  };

  const onRename = async (newRewriterId: string) => {
    renameRewriter(rewriterId, newRewriterId).then(() => {
      window.location.assign(`${_.last(parentBreadCrumbs)!.href}/${newRewriterId}`);
    });
    closeModal();
  };

  const onDelete = async () => {
    deleteRewriter(rewriterId, true).then((res) => {
      setTimeout(() => {
        window.location.assign(`${_.last(parentBreadCrumbs)!.href}`);
      }, 1000);
    });
    closeModal();
  };

  const deleteRewriterById = () => {
    setModalLayout(
      <DeleteObjectModal
        onConfirm={onDelete}
        onCancel={closeModal}
        title={`Delete ${rewriterId}`}
        message={`Are you sure you want to delete this Rule?`}
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
        'Rename Rule',
        'Cancel',
        'Rename',
        rewriterId,
        CREATE_REWRITER_MESSAGE
      )
    );
    showModal();
  };

  const onClone = async (newRewriterId: string) => {
    duplicateRewriter(rewriterId, newRewriterId).then(() => {
      window.location.assign(`${_.last(parentBreadCrumbs)!.href}/${newRewriterId}`);
    });
    closeModal();
  };

  const cloneRewriter = () => {
    setModalLayout(
      getCustomModal(
        onClone,
        closeModal,
        'Name',
        'Duplicate Rule',
        'Cancel',
        'Duplicate',
        rewriterId + ' (copy)',
        CREATE_REWRITER_MESSAGE
      )
    );
    showModal();
  };

  const rewriterMenu: EuiContextMenuPanelDescriptor[] = [
    {
      id: 0,
      title: 'Actions',
      items: [
        {
          name: 'Reload rule',
          onClick: () => {
            setRewriterMenuPopover(false);
            fetchRewriterById();
          },
        },
        {
          name: 'Rename rule',
          onClick: () => {
            setRewriterMenuPopover(false);
            renameRewriterById();
          },
        },
        {
          name: 'Duplicate rule',
          onClick: () => {
            setRewriterMenuPopover(false);
            cloneRewriter();
          },
        },
        {
          name: 'Delete rule',
          onClick: () => {
            setRewriterMenuPopover(false);
            deleteRewriterById();
          },
        },
      ],
    },
  ];

  // Rewriter Actions Button
  const rewriterActionsButton = (
    <EuiButton
      iconType="arrowDown"
      iconSide="right"
      onClick={() => setRewriterMenuPopover(!rewriterMenuPopover)}
    >
      Actions
    </EuiButton>
  );

  const saveButton = (
    <EuiButton fill iconType="save" onClick={onSave}>
      Save
    </EuiButton>
  );

  // Edit the breadcrumb when rewriter changes
  // Fetch therewriter on Initial Mount
  useEffect(() => {
    setBreadcrumbs([...ruleManagerViewBreadCrumb]);
    fetchRewriterById();
  }, []);

  // update breadcrumbs and rewriter content
  useEffect(() => {
    setBreadcrumbs([...ruleManagerViewBreadCrumb]);
    fetchRewriterById();
  }, [rewriterId]);
  return (
    <div>
      <EuiPage id="rewriterView">
        <EuiPageBody component="div">
          <EuiPageHeader>
            <EuiPageHeaderSection>
              <EuiTitle size="l">
                <h1>{rewriterId}</h1>
              </EuiTitle>
              <EuiFlexItem>
                <EuiSpacer size="s" />
              </EuiFlexItem>
            </EuiPageHeaderSection>
            <EuiPageHeaderSection>
              <EuiFlexGroup gutterSize="s">
                <EuiFlexItem grow={false}>
                  <EuiPopover
                    panelPaddingSize="none"
                    button={rewriterActionsButton}
                    isOpen={rewriterMenuPopover}
                    closePopover={() => setRewriterMenuPopover(false)}
                  >
                    <EuiContextMenu initialPanelId={0} panels={rewriterMenu} />
                  </EuiPopover>
                </EuiFlexItem>
                <EuiFlexItem>{saveButton}</EuiFlexItem>
              </EuiFlexGroup>
            </EuiPageHeaderSection>
          </EuiPageHeader>
          <EuiPageContentBody>
            {rewriterContent.class === _.keys(REWRITER_TYPE_MAP)[0] && (
              <CommonRulesView
                key={rewriterId + 'common_rules'}
                contentLoading={contentLoading}
                rewriterContent={rewriterContent}
                setconfigBody={setconfigBody}
                onSave={onSave}
              ></CommonRulesView>
            )}
          </EuiPageContentBody>
        </EuiPageBody>
      </EuiPage>
      {isModalVisible && modalLayout}
    </div>
  );
};
