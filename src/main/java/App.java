import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axis2.AxisFault;
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
        String argumentValues[] = {"iid=" + iid, "type=act*", "0"};
        OMElement activityMessageBuild = getBuildMessage("listEvents", argumentsTypes, argumentValues);
        OMElement activityResult = client.send(activityMessageBuild, "http://localhost:8080/ode/processes/InstanceManagement");
        OMElement activityEventList = activityResult.getFirstElement();

        argumentValues = new String[]{"iid=" + iid, "type=*Process*", "0"};
        OMElement processActivityMessageBuild = getBuildMessage("listEvents", argumentsTypes, argumentValues);
        OMElement processActivityResult = client.send(processActivityMessageBuild, "http://localhost:8080/ode/processes/InstanceManagement");
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

    public static void main(String[] args) {
        App app = new App();
        try {
            List<TEventInfo> eventsByInstanceId = app.getEventsByInstanceId("400");
            System.out.println("testing");
            app.getCommunicationbyInstanceId("400");
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }

        //org.apache.axis2.client.ServiceClient
        //OMElement root = client.buildMessage("listAllProcesses", new String[] {}, new String[] {});
        // OMElement root = client.buildMessage("getProcessInfo", new String[] {"processId"}, new String[] {"pid={http://bpelAdd.kit.edu}bpelAdd-1"});

//        OMElement activityEvents = app.client.buildMessage("listEvents", new String[]{"instanceFilter", "eventFilter", "maxCount"},
//                new String[]{"iid=400", "type=process* type=act*", "0"});
//        OMElement processEvents = client.buildMessage("listEvents", new String[]{"instanceFilter", "eventFilter", "maxCount"},
//                new String[]{"iid=400", "type=Process*", "0"});


    }

}
