import ActivityWizard from './activity-wizard';
import EventWizard from './event-wizard';
import ConditionWizard from './condition-wizard';
import {
    WizardRoot, SubWizardRoot, Stepper, Step, StepInner, StepCircle, StepTitle, StepSubtitle,
} from './styles';

const Wizard = ({
    activeStep,
    selectedForm,
    setSelectedForm,
}) => {
    const wizards = [
        <ActivityWizard {...{ selectedForm, setSelectedForm }} />,
        <EventWizard />,
        <ConditionWizard />,
    ];
    return (
        <WizardRoot>
            <Stepper>
                {[ 'Activity', 'Event', 'Condition' ].map((step, index) => (
                    <Step key={index}>
                        <StepInner>
                            <StepCircle step={index + 1} activeStep={activeStep} />
                            <StepTitle>{step}</StepTitle>
                            <StepSubtitle>{index > 0 && 'Optional'}</StepSubtitle>
                        </StepInner>
                    </Step>
                ))}
            </Stepper>
            <form id="wizard">
                { wizards.map((wizard, index) => (
                    <SubWizardRoot key={index} show={activeStep === (index + 1)}>
                        { wizard }
                    </SubWizardRoot>
                ))}
            </form>
        </WizardRoot>
    );
};
export default Wizard;
