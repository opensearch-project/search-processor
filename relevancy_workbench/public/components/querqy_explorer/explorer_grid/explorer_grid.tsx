import './explorer_grid.scss';
import { CoreStart } from '../../../../../../src/core/public';
import React, { useEffect, useState } from 'react';
import { Layout, Layouts, Responsive, WidthProvider } from 'react-grid-layout';
import { gridObjectType } from '../../../../common';
import { useObservable } from 'react-use';
import { GridObjectContainer } from '../grid_object_container/grid_object_container';
import _ from 'lodash';

// HOC container to provide dynamic width for Grid layout
const ResponsiveGridLayout = WidthProvider(Responsive);

interface ExplorerGirdProps {
  chrome: CoreStart['chrome'];
  gridObjects: gridObjectType[];
  editMode: boolean;
  onClickHandler: (value: string) => void;
  querqyResult: any;
}

export const ExplorerGrid = ({
  chrome,
  gridObjects,
  editMode,
  onClickHandler,
  querqyResult,
}: ExplorerGirdProps) => {
  const [currentLayout, setCurrentLayout] = useState<Layout[]>([]);
  const [postEditLayout, setPostEditLayout] = useState<Layout[]>([]);
  const [gridData, setGridData] = useState(gridObjects.map(() => <></>));
  const isLocked = useObservable(chrome.getIsNavDrawerLocked$());

  // Reset Size of Visualizations when layout is changed
  const layoutChanged = (currLayouts: Layout[], allLayouts: Layouts) => {
    window.dispatchEvent(new Event('resize'));
    console.log('here checking things!!', currLayouts);
    setPostEditLayout(currLayouts);
  };

  const loadGridObjects = () => {
    const gridDataComps = gridObjects.map((gridObject: gridObjectType, index) => (
      <>
        <GridObjectContainer
          objectType={gridObject.objectType}
          onClickHandler={onClickHandler}
          querqyResult={querqyResult}
        />
      </>
    ));
    setGridData(gridDataComps);
  };

  // Reload the Layout
  const reloadLayout = () => {
    const mapObjects = _.isEmpty(postEditLayout) ? gridObjects : postEditLayout;
    const tempLayout: Layout[] = mapObjects.map((gridObject) => {
      return {
        i: _.isEmpty(postEditLayout) ? gridObject.id : gridObject.i,
        x: gridObject.x,
        y: gridObject.y,
        w: gridObject.w,
        h: gridObject.h,
        static: !editMode,
      } as Layout;
    });
    setCurrentLayout(tempLayout);
  };

  // Reset Size of Panel Grid when Nav Dock is Locked
  useEffect(() => {
    setTimeout(function () {
      window.dispatchEvent(new Event('resize'));
    }, 300);
  }, [isLocked]);

  // Update layout whenever visualizations are updated
  useEffect(() => {
    console.log('init layout in usegrid', gridObjects);
    reloadLayout();
    loadGridObjects();
  }, [gridObjects, editMode]);

  useEffect(() => {
    loadGridObjects();
  }, [querqyResult]);

  useEffect(() => {
    loadGridObjects();
  }, []);

  return (
    <>
      <ResponsiveGridLayout
        layouts={{ lg: currentLayout, md: currentLayout, sm: currentLayout }}
        className="layout full-width"
        breakpoints={{ lg: 1200, md: 996, sm: 768, xs: 480, xxs: 0 }}
        cols={{ lg: 12, md: 12, sm: 12, xs: 1, xxs: 1 }}
        onLayoutChange={layoutChanged}
        // verticalCompact={false}
        compactType={null}
        // draggableHandle=".dragMe"
        // draggableCancel=".dontDragMe"
      >
        {gridObjects.map((gridObject: gridObjectType, index) => (
          <div key={gridObject.id}>{gridData[index]}</div>
        ))}
      </ResponsiveGridLayout>
    </>
  );
};
