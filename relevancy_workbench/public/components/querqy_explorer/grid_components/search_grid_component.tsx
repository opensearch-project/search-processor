import { EuiFieldSearch } from '@elastic/eui';
import React, { useState } from 'react';

interface SearchBarProps {
  onClickHandler: (value: string) => void;
}

export const SearchGridComponent = ({ onClickHandler }: SearchBarProps) => {
  const [searchValue, setSearchValue] = useState('');

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(e.target.value);
  };

  return (
    <EuiFieldSearch
      fullWidth={true}
      placeholder="Search this"
      value={searchValue}
      isClearable={true}
      onChange={onChange}
      onSearch={(value: string) => {
        onClickHandler(value);
      }}
    />
  );
};
