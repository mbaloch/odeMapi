import model.ProcessExec;
import model.WorkflowExec;
import org.apache.axiom.om.*;
import org.apache.axis2.AxisFault;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.ode.bpel.pmapi.*;
import org.apache.ode.utils.Namespaces;
import org.apache.xmlbeans.XmlException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by MUKHTAR on 6/8/2017.
 */
public class App {
    private ServiceClientUtil client = new ServiceClientUtil();

    void getCommunicationbyInstanceId(String iid) throws AxisFault {
        String argumentsTypes[]= {"getCommunication"};

        OMFactory _factory = OMAbstractFactory.getOMFactory();
        OMNamespace pmns = _factory.createOMNamespace(Namespaces.ODE_PMAPI_TYPES_NS, "ns");
        OMElement instanceIdElement = _factory.createOMElement("iid", pmns);
        instanceIdElement.setText(iid);
        Object argumentValues[] = {instanceIdElement};

        OMElement communicationMessageBuild = getBuildMessage("getCommunication", argumentsTypes, argumentValues);
        System.out.println(communicationMessageBuild);
        OMElement communicationResult = client.send(communicationMessageBuild, "http://localhost:8080/ode/processes/InstanceManagement");
        OMElement communicationResultFirstElement = communicationResult.getFirstElement();
        OMElement nextChildElement = communicationResultFirstElement.getFirstElement();
        communicationResultFirstElement.setNamespace(pmns);
        System.out.println(nextChildElement);

        try {
            GetCommunicationResponse getCommunicationResponse=
                    GetCommunicationResponse.Factory.parse(communicationResult.getXMLStreamReader());
//            GetCommunicationResponseDocument getCommunicationResponseDocument=
//                    GetCommunicationResponseDocument.Factory.parse(communicationResult.getXMLStreamReader());
            GetCommunicationResponseDocument getCommunicationResponseDocument2=
                    GetCommunicationResponseDocument.Factory.parse(communicationResultFirstElement.getXMLStreamReader());


            CommunicationType[] restoreInstanceArray = getCommunicationResponseDocument2.getGetCommunicationResponse().getRestoreInstanceArray();
            int length = restoreInstanceArray.length;
            CommunicationType communicationType = restoreInstanceArray[0];
            CommunicationType.Exchange[] exchangeArray = communicationType.getExchangeArray();

            System.out.println("abc");
//            GetCommunicationResponseDocument getCommunicationResponseDocument=
//                    GetCommunicationResponseDocument.Factory.parse(communicationResult.getXMLStreamReader());
        } catch (XmlException e) {
            e.printStackTrace();
        }
    }

    List<TEventInfo> getEventsByInstanceId(String iid) throws AxisFault {

        String argumentsTypes[] = {"instanceFilter", "eventFilter", "maxCount"};
        String argumentValues[] = {"iid=" + iid, "type=Act*", "0"};
        OMElement activityMessageBuild = getBuildMessage("listEvents", argumentsTypes, argumentValues);
        System.out.println("Message:"+activityMessageBuild);
        OMElement activityResult = client.send(activityMessageBuild, "http://balochsoft.com:8888/ode/processes/InstanceManagement");
        OMElement activityEventList = activityResult.getFirstElement();

        argumentValues = new String[]{"iid=" + iid, "type=*Process*", "0"};
        OMElement processActivityMessageBuild = getBuildMessage("listEvents", argumentsTypes, argumentValues);
        OMElement processActivityResult = client.send(processActivityMessageBuild, "http://balochsoft.com:8888/ode/processes/InstanceManagement");
        OMElement processActivityEventList = processActivityResult.getFirstElement();

        List<TEventInfo> tEventInfos = mergeEvents(Arrays.asList(activityEventList, processActivityEventList));

        return tEventInfos;
    }


    OMElement getBuildMessage(String operation, String argTypes[], Object argValues[]) {
        OMElement clientMessage = client.buildMessage(operation, argTypes, argValues);
        return clientMessage;
    }

    List<TEventInfo> mergeEvents(List<OMElement> eventList) {
        List<TEventInfo> eventInfoList = new ArrayList<>();
        for (OMElement events : eventList) {
            Iterator childElements = events.getChildElements();
            while (childElements.hasNext()) {
                Object next = childElements.next();
                OMElement omElement = (OMElement) next;
                EventInfoDocument eventInfoDocument = null;
                try {
                    eventInfoDocument = EventInfoDocument.Factory.parse(omElement.getXMLStreamReader());
                    TEventInfo eventInfo = eventInfoDocument.getEventInfo();
                    eventInfoList.add(eventInfo);
                } catch (XmlException e) {
                    e.printStackTrace();
                }


//                String name = eventInfo.getName();
//                long activityId = eventInfo.getActivityId();
//                String operation = eventInfo.getOperation();
//                String activityType = eventInfo.getActivityType();

            }

        }

        return eventInfoList;
    }
    List<ProcessExec> processExecList=new ArrayList<>();
    public Model generateRetrospective(List<TEventInfo> eventsByInstanceId){

    eventsByInstanceId.forEach(tEventInfo -> {
        switch (tEventInfo.getName()) {
            case "NewProcessInstanceEvent":
                processExecList.add(newProcessInstanceEventHandler(tEventInfo));
                break;
            case "ProcessInstanceStartedEvent":
                processInstanceStartedEventHandler(tEventInfo);
                break;
            case "ActivityExecStartEvent":
                ProcessExec processExec = activityExecStartEventHandler(tEventInfo);
                if (processExec !=null)
                    processExecList.add(processExec);
                break;
            case "VariableReadEvent":
                variableReadEventHandler(tEventInfo);
                break;
            case "VariableModificationEvent":
                variableModificationEventHandler(tEventInfo);
                break;
            case "ProcessMessageExchangeEvent":
                processMessageExchangeEventHandler(tEventInfo);
                break;

            default:

        }
    });
        eventsByInstanceId.forEach(tEventInfo -> {
            switch (tEventInfo.getName()) {
                case "ProcessCompletionEvent":
                    processCompletionEventHandler(tEventInfo);
                    break;
                case "ActivityExecEndEvent":
                    activityExecEndEventHandler(tEventInfo);
                    break;
                case "VariableReadEvent":
                    variableReadEventHandler(tEventInfo);
                    break;
                default:
            }
        });
        System.out.println("done");
        ProcessExec processExec1 = processExecList.stream().filter(processExec -> processExec instanceof WorkflowExec).findFirst().get();
        ObjectRDFMapper objectRDFMapper=new ObjectRDFMapper();
        Resource workflowExecResource = objectRDFMapper.createWorkflowExecResource(processExec1);
        processExecList.forEach(processExec -> {
            if(! (processExec instanceof WorkflowExec)){
                objectRDFMapper.createProcessExecResource(processExec,workflowExecResource);
            }
        });
        objectRDFMapper.rdfUtility.getModel().write(System.out);
        return null;
    }

    public Model generateRDFModel(){

        return null;
    }
    private WorkflowExec newProcessInstanceEventHandler(TEventInfo newProcessInstanceEvent) {

        WorkflowExec workflowExec=new WorkflowExec();
        workflowExec.setId(Long.toString(newProcessInstanceEvent.getActivityId()));
        workflowExec.setProcessName(newProcessInstanceEvent.getActivityName());
        workflowExec.setTitle(newProcessInstanceEvent.getProcessType().toString());
        workflowExec.setProcessId(newProcessInstanceEvent.getProcessId().toString());
        workflowExec.setStartTime(newProcessInstanceEvent.getTimestamp().toString());
        workflowExec.setProcessInstanceId(Long.toString( newProcessInstanceEvent.getInstanceId()));
        return workflowExec;
    }

    private void processInstanceStartedEventHandler(TEventInfo processInstanceStartEvent) {
//        logger.info(":::::::::::::::::::::::::Process Instance Started event start (Custome Handling)::::::::::::::::::::::::::::::::::::::::::");
//        logger.info("Time:" + processInstanceStartEvent.getTimestamp().toString());
//        logger.debug("ProcessName:(Custome Handling)" + processInstanceStartEvent.getProcessName());
//        logger.debug("Process Process Instance ID:(Custome Handling)" + processInstanceStartEvent.getProcessInstanceId());
//        logger.debug("Process ID:(Custome Handling)" + processInstanceStartEvent.getProcessId());
//        logger.debug("new process instance started event information:" + processInstanceStartEvent.toString());
//        logger.info(":::::::::::::::::::::::::Process Instance Started event end (Custome Handling)::::::::::::::::::::::::::::::::::::::::::");
    }

    private void processCompletionEventHandler(TEventInfo completionEvent) {
       processExecList.forEach(processExec -> {
           if (processExec instanceof WorkflowExec){
               processExec.setEndTime(completionEvent.getTimestamp().toString());
               processExec.setCompleted(true);
           }
       });

    }

    private ProcessExec activityExecStartEventHandler(TEventInfo activityExecStartEvent) {

        switch (activityExecStartEvent.getActivityType()) {
            case "OReply":
            case "OInvoke":
            case "OPickReceive":
                ProcessExec activityExec = new ProcessExec();
                activityExec.setId(Long.toString(activityExecStartEvent.getActivityId()));
                activityExec.setProcessName(activityExecStartEvent.getActivityName());
                activityExec.setTitle(activityExecStartEvent.getActivityName());
                activityExec.setProcessId(activityExecStartEvent.getProcessId().toString());
                activityExec.setStartTime(activityExecStartEvent.getTimestamp().toString());
                activityExec.setProcessInstanceId(Long.toString( activityExecStartEvent.getInstanceId()));
                System.out.println("hi");
                return activityExec;
            default:
        }

        return null;
    }

    private void activityExecEndEventHandler(TEventInfo activityExecEndEvent) {

        switch (activityExecEndEvent.getActivityType()) {
            case "OReply":
            case "OInvoke":
            case "OPickReceive":
                processExecList.forEach(processExec -> {
                    if (processExec.getId().equals(Long.toString(activityExecEndEvent.getActivityId()))){
                        processExec.setEndTime(activityExecEndEvent.getTimestamp().toString());
                        processExec.setCompleted(true);
                    }
                });
                break;
            default:

        }

    }

    private void variableReadEventHandler(TEventInfo variableReadEvent) {

    }

    private void variableModificationEventHandler(TEventInfo variableModificationEvent) {

    }
    private void processMessageExchangeEventHandler(TEventInfo processMessageExchangeEvent) {

    }
    RDFUtility rdfUtility=new RDFUtility();
    public static void main(String[] args) {
        App app = new App();
        try {
            List<TEventInfo> eventsByInstanceId = app.getEventsByInstanceId("400");
           // eventsByInstanceId.forEach(System.out::println);
            app.generateRetrospective(eventsByInstanceId);
            System.out.println("testing");
         //   app.getCommunicationbyInstanceId("400");
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }

    }

}
