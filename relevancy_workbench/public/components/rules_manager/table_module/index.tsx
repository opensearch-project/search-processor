import {
  EuiBreadcrumb,
  EuiFieldSearch,
  EuiHorizontalRule,
  EuiInMemoryTable,
  EuiLink,
  EuiTableFieldDataColumnType,
  EuiText,
} from '@elastic/eui';
import _ from 'lodash';
import React from 'react';

interface TableModuleProps {
  parentBreadCrumbs: EuiBreadcrumb[];
  searchQuery: string;
  setSearchQuery: React.Dispatch<React.SetStateAction<string>>;
  setSelectedRewriters: React.Dispatch<React.SetStateAction<any[]>>;
  rewriterList: any[];
  tableLoading: boolean;
}

export const TableModule = ({
  parentBreadCrumbs,
  searchQuery,
  setSearchQuery,
  setSelectedRewriters,
  rewriterList,
  tableLoading,
}: TableModuleProps) => {
  const tableColumns = [
    {
      field: 'rewriterId',
      name: 'Rewriter Name',
      sortable: true,
      truncateText: true,
      render: (value, record) => (
        <EuiLink href={`${_.last(parentBreadCrumbs)!.href}/${record.rewriterId}`}>
          {_.truncate(value, { length: 100 })}
        </EuiLink>
      ),
    },
    {
      field: 'rewriterType',
      name: 'Type',
      sortable: true,
      render: (value) => <EuiText>{value}</EuiText>,
    },
  ] as Array<EuiTableFieldDataColumnType<any>>;

  return (
    <>
      <EuiFieldSearch
        fullWidth
        data-test-subj="rewriterSearchBar"
        placeholder="Search rewriter name name"
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
      />
      <EuiHorizontalRule margin="m" />
      <EuiInMemoryTable
        loading={tableLoading}
        items={
          searchQuery
            ? rewriterList.filter((rewriter) =>
                rewriter.rewriterId.toLowerCase().includes(searchQuery.toLowerCase())
              )
            : rewriterList
        }
        itemId="rewriterId"
        columns={tableColumns}
        tableLayout="auto"
        pagination={{
          initialPageSize: 10,
          pageSizeOptions: [8, 10, 13],
        }}
        sorting={{
          sort: {
            field: 'dateModified',
            direction: 'desc',
          },
        }}
        allowNeutralSort={false}
        isSelectable={true}
        selection={{
          onSelectionChange: (items) => setSelectedRewriters(items),
        }}
      />
    </>
  );
};
