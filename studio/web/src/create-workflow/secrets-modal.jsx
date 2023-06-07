import { atoms } from '../core/atoms';
import {
    Button, Modal, ModalTitle, ModalBody, ModalFooter, TextField,
} from "@symphony-ui/uitoolkit-components/components";
import { useEffect, useState } from 'react';
import { useRecoilState } from 'recoil';
import api from '../core/api';
import Spinner from '../core/spinner';
import styled from 'styled-components';

const Table = styled.table`
    border-collapse: collapse;
    th, td { padding: .4rem 1rem }
    th {
        position: sticky;
        top: 0;
        z-index: 2;
        text-align: left;
        color: #fff;
        background-color: var(--tk-color-electricity-70);
    }
    tr:nth-child(even) {
        background-color: rgba(150, 150, 150, .2);
    }
    td[data-copy] {
        cursor: pointer;
        :after {
            font-family: tk-icons !important;
            content: '';
            font-size: .9rem;
            padding-left: .5rem;
            display: inline;
            visibility: hidden;
        }
    }
    td[data-copy]:hover:after {
        visibility: visible;
    }
`;

const SecretsModal = ({ setShow }) => {
    const [ loading, setLoading ] = useRecoilState(atoms.loading);
    const [ secrets, setSecrets ] = useState();
    const [ newSecret, setNewSecret ] = useState({ key: '', secret: '' });
    const { listSecrets, addSecret, deleteSecret, showStatus } = api();

    useEffect(() => listSecrets((response) => setSecrets(response)), []);

    const submitNewSecret = () => {
        if (newSecret.key.trim().length === 0 || newSecret.secret.trim().length === 0) {
            showStatus(true, 'Secret key and value cannot be empty');
            return;
        }
        const data = newSecret;
        data.key = data.key.trim();
        data.secret = data.secret.trim();

        setLoading(true);
        addSecret(data, () => {
            listSecrets((response) => setSecrets(response));
            setLoading(false);
            showStatus(false, `New secret ${newSecret.key} added`);
            setNewSecret({ key: '', secret: '' });
        });
    };

    const copy = (key) => {
        navigator.clipboard.writeText(`\${secret('${key}')}`);
        showStatus(false, 'Copied to clipboard');
    };

    const SecretsGrid = () => {
        const [ deleteLoading, setDeleteLoading ] = useState(false);
        const [ selectedKey, setSelectedKey ] = useState();

        const processDelete = (secretKey) => {
            setDeleteLoading(true);
            deleteSecret(secretKey, () => {
                setSecrets((old) => old.filter((s) => s.secretKey !== secretKey));
                showStatus(false, `Secret ${newSecret.key} deleted`);
                setDeleteLoading(false);
                setSelectedKey(undefined);
            });
        };

        if (secrets.length === 0) {
            return 'No Secrets';
        }
        return (
            <>
                <Table>
                    <thead>
                        <tr>
                            <th>Secret Key</th>
                            <th>Created At</th>
                            <th>Delete</th>
                        </tr>
                    </thead>
                    <tbody>
                        { secrets.map(({ createdAt, secretKey }) => (
                            <tr key={secretKey}>
                                <td data-copy onClick={() => copy(secretKey)}>
                                    {secretKey}
                                </td>
                                <td>{(new Date(createdAt)).toLocaleString()}</td>
                                <td>
                                    <Button
                                        size="small"
                                        variant="secondary-destructive"
                                        onClick={() => setSelectedKey(secretKey)}
                                    >
                                        Delete
                                    </Button>
                                </td>
                            </tr>
                        )) }
                    </tbody>
                </Table>
                <Modal show={!!selectedKey} closeButton onClose={() => setSelectedKey(undefined)}>
                    <ModalTitle>Confirm Delete</ModalTitle>
                    <ModalBody>
                        This will delete the secret <em>{selectedKey}</em> permanently.
                        Are you sure?
                    </ModalBody>
                    <ModalFooter>
                        <Button
                            variant="destructive"
                            onClick={() => processDelete(selectedKey)}
                            loading={deleteLoading}
                        >
                            Delete
                        </Button>
                        <Button
                            variant="secondary"
                            onClick={() => setSelectedKey(undefined)}
                            disabled={deleteLoading}
                        >
                            Cancel
                        </Button>
                    </ModalFooter>
                </Modal>
            </>
        )
    };

    return (
        <Modal size="large" show closeButton onClose={() => setShow(false)}>
            <ModalTitle>Manage Secrets</ModalTitle>
            <ModalBody style={{ minHeight: '17rem', display: 'flex', alignItems: 'flex-start' }}>
                { !secrets ? <Spinner /> : <SecretsGrid /> }
            </ModalBody>
            <ModalFooter style={{ justifyContent: 'flex-start', alignItems: 'flex-end' }}>
                <TextField
                    showRequired
                    label="Secret Key"
                    placeholder="my-key"
                    disabled={loading}
                    value={newSecret.key}
                    onChange={({ target }) => setNewSecret((s) => ({ ...s, key: target.value }))}
                    maxLength={15}
                />
                <TextField
                    showRequired
                    isMasked
                    label="Secret Value"
                    placeholder="my-secret"
                    disabled={loading}
                    value={newSecret.secret}
                    onChange={({ target }) => setNewSecret((s) => ({ ...s, secret: target.value }))}
                />
                <Button
                    onClick={submitNewSecret}
                    disabled={loading || newSecret.key.trim().length === 0 || newSecret.secret.trim().length === 0}
                >
                    Add Secret
                </Button>
            </ModalFooter>
        </Modal>
    );
};
export default SecretsModal;
