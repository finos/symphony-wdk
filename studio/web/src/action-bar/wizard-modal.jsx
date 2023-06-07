import { atoms } from '../core/atoms';
import {
    Button, Modal, ModalBody, ModalFooter, ModalTitle
} from '@symphony-ui/uitoolkit-components/components';
import { useRecoilState } from 'recoil';
import { useState, useEffect } from 'react';
import Wizard from '../wizard/wizard';

const eventFields = {
    'message-received': 'content',
    'form-replied': 'form-id',
    'activity-completed': 'activity-id',
    'activity-expired': 'activity-id',
};

const kebabCase = (string) => string
    .replace(/([a-z])([A-Z])/g, "$1-$2")
    .replace(/[\s_]+/g, '-')
    .toLowerCase();

const formatValue = (value) => {
    if (value.indexOf('\n') === -1) {
        return value;
    }
    return value.split('\n').map((line) => `        ${line}`).join('\n');
};

const WizardModal = ({ setShow }) => {
    const [ isNextDisabled, setNextDisabled ] = useState(true);
    const [ selectedForm, setSelectedForm ] = useState();
    const [ activeStep, setActiveStep ] = useState(1);
    const setSnippet = useRecoilState(atoms.snippet)[1];

    const addCodeSnippet = () => {
        const data = Object.fromEntries(new FormData(document.querySelector('#wizard')).entries());
        const swadlBuilder = [
            `- ${selectedForm.activity}:`,
            ...selectedForm.fields
                .filter(({ key }) => data[key]?.trim().length > 0)
                .map(({ key }) => `      ${kebabCase(key)}: ${data[key].indexOf('\n') > -1 ? '|\n' : ''}${formatValue(data[key])}`),
        ];
        if (data.condition.trim().length > 0) {
            swadlBuilder.splice(2, 0, `      if: \${${data.condition.trim()}}`);
        }
        if (data.eventValue.trim().length > 0) {
            swadlBuilder.splice(2, 0, ...[
                '      on:',
                `        ${data.event}:`,
                `          ${eventFields[data.event]}: ${data.eventValue.trim()}`,
            ]);
        }
        setSnippet(swadlBuilder.join('\n') + '\n\n  ');
        setShow(false);
    };

    useEffect(() => {
        if (selectedForm) {
            document.querySelector('#wizard').onkeyup =
                () => setNextDisabled(shouldNextBeDisabled());
        }
    }, [ selectedForm ]);

    const shouldNextBeDisabled = () => {
        if (activeStep === 1) {
            const data = Object.fromEntries(new FormData(document.querySelector('#wizard')).entries());
            const emptyValues = selectedForm.fields
                .filter(({ props }) => props.showRequired)
                .find(({ key }) => data[key]?.trim() === '');
            return !!emptyValues;
        }
        return false;
    };

    return (
        <Modal size="large" show closeButton onClose={() => setShow(false)}>
            <ModalTitle>SWADL Generator Wizard</ModalTitle>
            <ModalBody>
                <Wizard {...{
                    activeStep,
                    selectedForm,
                    setSelectedForm,
                }} />
            </ModalBody>
            <ModalFooter>
                <Button
                    variant="secondary"
                    onClick={() => activeStep === 1 ? setSelectedForm(undefined) : setActiveStep((a) => a - 1)}
                    disabled={!selectedForm && activeStep < 2}
                >
                    Back
                </Button>
                <Button
                    variant="secondary"
                    onClick={() => setActiveStep((a) => a + 1)}
                    disabled={!selectedForm || activeStep >= 3 || isNextDisabled}
                >
                    Next
                </Button>
                <Button
                    variant="primary"
                    onClick={addCodeSnippet}
                    disabled={isNextDisabled}
                >
                    Get Code
                </Button>
                <Button
                    variant="secondary"
                    onClick={() => setShow(false)}
                >
                    Cancel
                </Button>
            </ModalFooter>
        </Modal>
    );
};
export default WizardModal;
