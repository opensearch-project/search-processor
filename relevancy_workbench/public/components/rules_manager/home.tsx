import { EuiGlobalToastList } from '@elastic/eui';
import { Toast } from '@elastic/eui/src/components/toast/global_toast_list';
import { StaticContext } from 'react-router';
import React, { ReactChild, useState } from 'react';
import { Route, RouteComponentProps } from 'react-router-dom';
import { RelevancySideBar } from '../common_utils/side_nav';
import { RuleManagerTable } from './rule_manager_table';
import { RuleManagerView } from './rule_manger_view';
import { ChromeBreadcrumb, CoreStart } from '../../../../../src/core/public';
import {
  BASE_RELEVENCY_WORKBENCH_API,
  PLUGIN_RULES_MANAGER_NAME,
  PLUGIN_RULES_MANAGER_URL,
} from '../../../common';
import { isNameValid } from '../common_utils/utils';
import _ from 'lodash';

interface RulesManagerProps {
  parentBreadCrumbs: ChromeBreadcrumb[];
  notifications: CoreStart['notifications'];
  http: CoreStart['http'];
  setBreadcrumbs: (newBreadcrumbs: ChromeBreadcrumb[]) => void;
  renderProps: RouteComponentProps<any, StaticContext, any>;
  setToast: (title: string, color?: string, text?: any, side?: string) => void;
}
export const Home = ({
  parentBreadCrumbs,
  notifications,
  http,
  setBreadcrumbs,
  renderProps,
  setToast,
}: RulesManagerProps) => {
  // const [toasts, setToasts] = useState<Toast[]>([]);
  // const [toastRightSide, setToastRightSide] = useState<boolean>(true);
  const [rewriterList, setRewriterList] = useState<any[]>([]);
  const [tableLoading, setTableLoading] = useState(false);

  const rulesManagerBreadCrumb = [
    ...parentBreadCrumbs,
    {
      text: `${PLUGIN_RULES_MANAGER_NAME}`,
      href: `#${PLUGIN_RULES_MANAGER_URL}`,
    },
  ];

  // const setToast = (title: string, color = 'success', text?: ReactChild, side?: string) => {
  //   if (!text) text = '';
  //   setToastRightSide(!side ? true : false);
  //   setToasts([...toasts, { id: new Date().toISOString(), title, text, color } as Toast]);
  // };

  const fetchAllRewriters = () => {
    setTableLoading(true);
    http
      .get(`${BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER}`)
      .then((res) => {
        console.log(res);
        setRewriterList(res.rewriters);
      })
      .catch((error: Error) => {
        console.error(error);
      })
      .finally(() => {
        setTableLoading(false);
      });
  };

  // Creates a new Rewriter
  const createRewriter = (newRewriterId: string, newRewriterType: string) => {
    if (!isNameValid(newRewriterId)) {
      setToast('Invalid Rewriter name', 'danger');
      return;
    }

    return http
      .post(BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER_CREATE, {
        body: JSON.stringify({
          rewriterId: newRewriterId,
          rewriterType: newRewriterType,
        }),
      })
      .then(async (res) => {
        setToast(`Rewriter "${newRewriterId}" successfully created!`);
        window.location.assign(`${_.last(rulesManagerBreadCrumb)!.href}/${res.rewriterId}`);
      })
      .catch((err) => {
        setToast('Please ask your administrator to enable Rewriters for you.', 'danger');
        console.error(err);
      });
  };

  // Renames an existing Rewriter
  const renameRewriter = (currentRewriterId: string, newRewriterId: string) => {
    if (!isNameValid(newRewriterId)) {
      setToast('Invalid rewriter name', 'danger');
      return Promise.reject();
    }
    return http
      .put(`${BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER_RENAME}`, {
        body: JSON.stringify({
          currentRewriterId: currentRewriterId,
          newRewriterId: newRewriterId,
        }),
      })
      .then((res) => {
        setRewriterList((preRewriterList) => {
          const newRewriterList = [...preRewriterList];
          const renamedRewriter = newRewriterList.find(
            (rewriter) => rewriter.rewriterId === currentRewriterId
          );
          if (renamedRewriter) renamedRewriter.rewriterId = newRewriterId;
          return newRewriterList;
        });
        setToast(`Rewriter successfully renamed into "${newRewriterId}"`);
      })
      .catch((err) => {
        setToast(
          'Error renaming Rewriter, please make sure you have the correct permission.',
          'danger'
        );
        console.error(err.body.message);
      });
  };

  // Delete an existing Rewriter
  const deleteRewriter = (rewriterId: string, showToast: boolean) => {
    return http
      .delete(`${BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER}/${rewriterId}`)
      .then((res) => {
        setRewriterList((preRewriterList) => {
          const newList = [...preRewriterList];
          _.remove(newList, (rewriter) => rewriter.rewriterId === rewriterId);
          return newList;
        });
        if (showToast) setToast(`Rewriter "${rewriterId}" successfully deleted`);
        return res;
      })
      .catch((err) => {
        setToast(
          'Error deleting rewriter, please make sure you have the correct permission.',
          'danger'
        );
        console.error(err.body.message);
      });
  };

  // Duplicate an existing Rewriter
  const duplicateRewriter = (currentRewriterId: string, newRewriterId: string) => {
    if (!isNameValid(newRewriterId)) {
      setToast('Invalid rewriter name', 'danger');
      return Promise.reject();
    }
    return http
      .post(`${BASE_RELEVENCY_WORKBENCH_API.RULES_MANAGER_CLONE}`, {
        body: JSON.stringify({
          currentRewriterId: currentRewriterId,
          newRewriterId: newRewriterId,
        }),
      })
      .then((res) => {
        setRewriterList((preRewriterList) => {
          return [
            ...preRewriterList,
            {
              rewriterId: res.rewriterId,
              rewriterType: _.filter(preRewriterList, { rewriterId: currentRewriterId })[0]
                .rewriterType,
            },
          ];
        });
        setToast(`Rewriter "${newRewriterId}" successfully deleted`);
      })
      .catch((err) => {
        setToast(
          'Error deleting rewriter, please make sure you have the correct permission.',
          'danger'
        );
        console.error(err.body.message);
      });
  };

  return (
    <div>
      <Route
        exact
        path={renderProps.match.path}
        render={(props) => {
          return (
            <RelevancySideBar>
              <RuleManagerTable
                parentBreadCrumbs={rulesManagerBreadCrumb}
                notifications={notifications}
                http={http}
                setBreadcrumbs={setBreadcrumbs}
                fetchAllRewriters={fetchAllRewriters}
                rewriterList={rewriterList}
                tableLoading={tableLoading}
                renameRewriter={renameRewriter}
                deleteRewriter={deleteRewriter}
                setToast={setToast}
                duplicateRewriter={duplicateRewriter}
                createRewriter={createRewriter}
              ></RuleManagerTable>
            </RelevancySideBar>
          );
        }}
      />

      <Route
        path={`${renderProps.match.path}/:id`}
        render={(props) => {
          return (
            <RuleManagerView
              rewriterId={props.match.params.id}
              parentBreadCrumbs={rulesManagerBreadCrumb}
              http={http}
              setBreadcrumbs={setBreadcrumbs}
              renameRewriter={renameRewriter}
              deleteRewriter={deleteRewriter}
              setToast={setToast}
              duplicateRewriter={duplicateRewriter}
              createRewriter={createRewriter}
            ></RuleManagerView>
          );
        }}
      />
    </div>
  );
};
