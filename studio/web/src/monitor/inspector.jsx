import {
    Button, Modal, ModalBody, ModalFooter, ModalTitle,
} from '@symphony-ui/uitoolkit-components/components';
import { useRecoilState } from 'recoil';
import { atoms } from '../core/atoms';
import a11yDark from 'react-syntax-highlighter/dist/esm/styles/hljs/a11y-dark';
import a11yLight from 'react-syntax-highlighter/dist/esm/styles/hljs/a11y-light';
import json from 'react-syntax-highlighter/dist/esm/languages/hljs/json';
import { Light as SyntaxHighlighter } from 'react-syntax-highlighter';

SyntaxHighlighter.registerLanguage('json', json);

const Inspector = ({ payload, setShow }) => {
    const theme = useRecoilState(atoms.theme)[0];
    return (
        <Modal size="full-width" show closeButton onClose={() => setShow(false)}>
            <ModalTitle>Payload Inspector</ModalTitle>
            <ModalBody>
                <SyntaxHighlighter
                    showLineNumbers
                    children={JSON.stringify(payload, null, 2)}
                    language="json"
                    style={theme === 'light' ? a11yLight : a11yDark}
                    customStyle={{ height: 'calc(100vh - 17rem)' }}
                />
            </ModalBody>
            <ModalFooter>
                <Button
                    variant="secondary"
                    onClick={() => setShow(false)}
                >
                    Close
                </Button>
            </ModalFooter>
        </Modal>
    );
};
export default Inspector;
