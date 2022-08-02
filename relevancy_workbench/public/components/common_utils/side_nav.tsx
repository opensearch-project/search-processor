/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import { EuiPage, EuiPageBody, EuiPageSideBar, EuiSideNav, EuiSideNavItemType } from '@elastic/eui';
import React from 'react';

export function RelevancySideBar(props: { children: React.ReactNode }) {
  // set items.isSelected based on location.hash passed in
  // tries to find an item where href is a prefix of the hash
  // if none will try to find an item where the hash is a prefix of href
  function setIsSelected(
    items: EuiSideNavItemType<React.ReactNode>[],
    hash: string,
    initial = true,
    reverse = false
  ): boolean {
    // Default page is Events Analytics
    // But it is kept as second option in side nav
    if (hash === '#/') {
      items[0].items[0].isSelected = true;
      return true;
    }
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      if (item.href && ((reverse && item.href.startsWith(hash)) || hash.startsWith(item.href))) {
        item.isSelected = true;
        return true;
      }
      if (item.items?.length && setIsSelected(item.items, hash, false, reverse)) return true;
    }
    return initial && setIsSelected(items, hash, false, !reverse);
  }

  const items = [
    {
      name: 'Relevancy Workbench',
      id: 0,
      items: [
        {
          name: 'Query Explorer',
          id: 1,
          href: '#/querqy_explorer',
        },
        {
          name: 'Rules Manager',
          id: 2,
          href: '#/rules_manager',
        },
      ],
    },
  ];
  setIsSelected(items, location.hash);

  return (
    <EuiPage>
      <EuiPageSideBar>
        <EuiSideNav items={items} />
      </EuiPageSideBar>
      <EuiPageBody>{props.children}</EuiPageBody>
    </EuiPage>
  );
}
