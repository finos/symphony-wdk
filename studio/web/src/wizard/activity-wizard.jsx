import React from 'react';
import { TextField, TextArea } from '@symphony-ui/uitoolkit-components/components';
import { ActivityRoot, Items, Item } from './styles';
import ExampleTemplates from './example-templates';

const fields = {
    id: <TextField key="id" name="id" label="Identifier" showRequired />,
    streamId: <TextField key="streamId" name="streamId" label="Stream ID" />,
    messageId: <TextField key="messageId" name="messageId" label="Message ID" showRequired />,
    url: <TextField key="url" name="url" label="URL" showRequired />,
    roomName: <TextField key="roomName" name="roomName" label="Room Name" showRequired />,
    roomDescription: <TextField key="roomDescription" name="roomDescription" label="Room Description" showRequired />,
    content: <TextArea key="content" name="content" label="Content" showRequired />,
    script: <TextArea key="script" name="script" label="Script" showRequired />,
    examples: <ExampleTemplates key="examples" />,
};

const forms = [
    {
        label: 'Send a message',
        activity: 'send-message',
        fields: [ fields.id, fields.streamId, fields.content, fields.examples ],
    },
    {
        label: 'Update a message',
        activity: 'update-message',
        fields: [ fields.id, fields.messageId, fields.content, fields.examples ] },
    {
        label: 'Create a room',
        activity: 'create-room',
        fields: [ fields.id, fields.roomName, fields.roomDescription ] },
    {
        label: 'Call an API',
        activity: 'execute-request',
        fields: [ fields.id, fields.url ] },
    {
        label: 'Execute a script',
        activity: 'execute-script',
        fields: [ fields.id, fields.script ],
    },
];

const Menu = ({ setSelectedForm }) => (
    <>
        Select a type of activity to execute
        <Items>
            { forms.map((form) => (
                <Item key={form.label} onClick={() => setSelectedForm(form)}>
                    { form.label }
                </Item>
            )) }
        </Items>
    </>
);

const ActivityWizard = ({ selectedForm, setSelectedForm }) =>
    !selectedForm ? <Menu {...{ setSelectedForm }} /> : (
        <ActivityRoot>
            { selectedForm.label }
            { selectedForm.fields }
        </ActivityRoot>
    );
export default ActivityWizard;
