import { atom } from 'recoil';

export const atoms = {
    loading: atom({ key: 'loading', default: false }),
    status: atom({ key: 'status', default: { show: false }}),
    session: atom({ key: 'session', default: undefined }),
    workflows: atom({ key: 'workflows', default: undefined }),
    currentWorkflow: atom({ key: 'currentWorkflow', default: undefined }),
    editMode: atom({ key: 'editMode', default: true }),
    isContentChanged: atom({ key: 'isContentChanged', default: 'original' }),
    selectedInstance: atom({ key: 'selectedInstance', default: undefined }),
    activeVersion: atom({ key: 'activeVersion', default: undefined }),
    contents: atom({ key: 'contents', default: undefined }),
    author: atom({ key: 'author', default: undefined }),
    position: atom({ key: 'position', default: undefined }),
    snippet: atom({ key: 'snippet', default: {} }),
    markers: atom({ key: 'markers', default: [] }),
    logs: atom({ key: 'logs', default: '' }),
    theme: atom({ key: 'theme', default: 'light' }),
};
