/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import {
  EuiButtonEmpty,
  EuiForm,
  EuiModal,
  EuiModalBody,
  EuiModalFooter,
  EuiModalHeader,
  EuiModalHeaderTitle,
  EuiOverlayMask,
  EuiFormRow,
  EuiFieldText,
  EuiButton,
  EuiSelect,
} from '@elastic/eui';
import { ChangeEvent } from 'react';
import { REWRITER_TYPE_MAP } from '../../../../common';
import _ from 'lodash';

/*
 * "CustomInputModalProps" component is used to create a modal with an input filed
 *
 * Props taken in as params are:
 * runModal - function to fetch input field value and trigger closing modal
 * closeModal - function to trigger closing modal
 * titletxt - string as header for title of modal
 * labelTxt - string as header for input field
 * btn1txt - string as content to fill "close button"
 * btn2txt - string as content to fill "confirm button"
 * openObjectName - Default input value for the field
 * helpText - string help for the input field
 * optionalArgs - Arguments needed to pass them to runModal function
 */

type CustomInputModalProps = {
  runModal:
    | ((value: string, value2: string, value3: string, value4: string) => void)
    | ((value: string) => void);
  closeModal: (
    event?: React.KeyboardEvent<HTMLDivElement> | React.MouseEvent<HTMLButtonElement, MouseEvent>
  ) => void;
  labelTxt: string;
  titletxt: string;
  btn1txt: string;
  btn2txt: string;
  openObjectName?: string;
  helpText?: string;
  optionalArgs?: string[];
  isCreateRewriter?: boolean;
};

export const CustomInputModal = (props: CustomInputModalProps) => {
  const {
    runModal,
    closeModal,
    labelTxt,
    titletxt,
    btn1txt,
    btn2txt,
    openObjectName,
    helpText,
    optionalArgs,
    isCreateRewriter,
  } = props;
  const [value, setValue] = useState(openObjectName || ''); // sets input value

  const selectOptions = _.keys(REWRITER_TYPE_MAP).map((fieldName) => {
    return { value: fieldName, text: fieldName };
  });
  const [selectValue, setSelectValue] = useState(selectOptions[0].value);

  const onChangeSelect = (e: ChangeEvent<HTMLSelectElement>) => {
    setSelectValue(e.target.value);
  };

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setValue(e.target.value);
  };

  return (
    <EuiOverlayMask>
      <EuiModal onClose={closeModal} initialFocus="[name=input]">
        <EuiModalHeader>
          <EuiModalHeaderTitle>{titletxt}</EuiModalHeaderTitle>
        </EuiModalHeader>

        <EuiModalBody>
          <EuiForm>
            <EuiFormRow label={labelTxt} helpText={helpText}>
              <EuiFieldText
                data-test-subj="customModalFieldText"
                name="input"
                value={value}
                onChange={(e) => onChange(e)}
              />
            </EuiFormRow>
            {isCreateRewriter && (
              <EuiFormRow label={'Select rewriter type'}>
                <EuiSelect
                  id="custom_model_create_selector"
                  options={selectOptions}
                  value={selectValue}
                  onChange={(e) => onChangeSelect(e)}
                  aria-label="select rewriter type"
                />
              </EuiFormRow>
            )}
          </EuiForm>
        </EuiModalBody>

        <EuiModalFooter>
          <EuiButtonEmpty onClick={closeModal}>{btn1txt}</EuiButtonEmpty>
          {optionalArgs === undefined ? (
            <EuiButton
              data-test-subj="runModalButton"
              onClick={() => {
                if (isCreateRewriter) {
                  runModal(value, selectValue);
                } else {
                  runModal(value);
                }
              }}
              fill
            >
              {btn2txt}
            </EuiButton>
          ) : (
            <EuiButton
              data-test-subj="runModalButton"
              onClick={() => runModal(value, ...optionalArgs)}
              fill
            >
              {btn2txt}
            </EuiButton>
          )}
        </EuiModalFooter>
      </EuiModal>
    </EuiOverlayMask>
  );
};
