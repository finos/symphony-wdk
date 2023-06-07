import styled from 'styled-components';

const WizardRoot = styled.div`
    padding: .5rem;
    height: 32rem;
`;

const SubWizardRoot = styled.div`
    display: ${props => props.show ? 'flex' : 'none'};
    flex-direction: column;
    gap: .6rem;
    margin: 1rem;
`;

const ActivityRoot = styled.div`
    display: flex;
    flex-direction: column;
    gap: .6rem;
`;

const Stepper = styled.div`
    display: flex;
    justify-content: center;
    padding: 1rem 3rem;
`;

const Step = styled.div`
    display: flex;
    align-items: center;

    &:not(:last-child):after {
        display: flex;
        flex-direction: row;
        content: ' ';
        height: 1px;
        background: var(--tk-color-graphite-30);
        width: 6rem;
        margin: 0 1rem;
    }
`;

const StepInner = styled.div`
    display: flex;
    flex-direction: column;
    align-items: center;
`;

const StepCircle = styled.div`
    display: flex;
    justify-content: center;
    align-items: center;
    font-weight: 600;
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    color: #fff;
    background: ${props => props.step === props.activeStep ? 'var(--tk-color-red-40)' : 'var(--tk-color-graphite-40)'};
    margin-bottom: .6rem;
    &:after { content: '${props => props.step}' }
`;

const StepTitle = styled.div`
    font-weight: 600;
`;

const StepSubtitle = styled.div`
    font-size: .8rem;
    min-height: 1rem;
`;

const Items = styled.div`
    display: flex;
    flex-direction: column;
    gap: .6rem;
`;

const Item = styled.div`
    font-size: .9rem;
    padding: .8rem;
    border: 1px solid transparent;
    &:hover {
        border-radius: .5rem;
        border: 1px solid var(--tk-color-graphite-30);
        cursor: pointer;
    }
`;

export {
    WizardRoot, SubWizardRoot, ActivityRoot, Stepper, Step, StepInner,
    StepCircle, StepTitle, StepSubtitle, Items, Item,
}
