import {
    Button, Modal, ModalBody, ModalFooter, ModalTitle,
} from '@symphony-ui/uitoolkit-components/components';
import Diagram from '../diagram/diagram';

const DiagramModal = ({ setShow }) => (
    <Modal size="full-width" show closeButton onClose={() => setShow(false)}>
        <ModalTitle>Diagram</ModalTitle>
        <ModalBody>
            <Diagram />
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

export default DiagramModal;
