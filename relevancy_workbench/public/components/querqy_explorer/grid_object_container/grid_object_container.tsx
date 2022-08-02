import React, { useEffect, useState } from 'react';
import { ResultGridComponent } from '../grid_components/result_grid_component';
import { SearchGridComponent } from '../grid_components/search_grid_component';

interface GridObjectContainerProps {
  objectType: string;
  //   searchToken: string;
  //   setSearchToken: React.Dispatch<React.SetStateAction<string>>;
  onClickHandler: (value: string) => void;
  querqyResult: any;
}

export const GridObjectContainer = ({
  objectType,
  //   searchToken,
  //   setSearchToken,
  onClickHandler,
  querqyResult,
}: GridObjectContainerProps) => {
  const searchBar = <SearchGridComponent onClickHandler={onClickHandler} />;
  const resultGrid = <ResultGridComponent querqyResult={querqyResult} />;

  useEffect(() => {
    console.log('query result changed in explorer gridContainer');
  }, [querqyResult]);

  const populateContainer = () => {
    switch (objectType) {
      case 'searchBar':
        return searchBar;
      case 'resultGrid':
        return resultGrid;
      default:
        return <></>;
    }
  };

  return <>{populateContainer()}</>;
};
