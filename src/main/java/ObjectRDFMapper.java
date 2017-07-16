import model.ProcessExec;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by mukhtar on 16.07.17.
 */
public class ObjectRDFMapper {

    public RDFUtility getRdfUtility() {
        return rdfUtility;
    }

    RDFUtility rdfUtility = new RDFUtility();
    public Resource createWorkflowExecResource(ProcessExec processExec) {
        Resource workflowExecResource = rdfUtility.createProcessExec(
                processExec.getProcessInstanceId(), processExec.getProcessId(), processExec.getId()
                , processExec.getStartTime(), processExec.getEndTime(),
                processExec.getCompleted().toString());
      //  rdfUtility.getModel().write(System.out);
        return workflowExecResource;
    }
    public Resource createProcessExecResource( ProcessExec processExec, Resource workflowResource) {

        Resource processExecResource = rdfUtility.createProcessExec(
                processExec.getProcessInstanceId(), processExec.getTitle(), processExec.getId()
                , processExec.getStartTime(), processExec.getEndTime(),
                processExec.getCompleted().toString());
        Property isPartOf = rdfUtility.isPartOf(processExecResource, workflowResource);
        return processExecResource;

        /*
        //if (subject != null) {
        if (processExec.getCompleted() == null)
            processExec.setCompleted(false);
        RDFUtility rdfUtility = new RDFUtility();
        Resource processExecResource = rdfUtility.createProcessExec(
                shortUUID(), processExec.getTitle(), processExec.getId()
                , processExec.getStartTime(), processExec.getEndTime(),
                processExec.getCompleted().toString());
        Property wasAssociatedWith = rdfUtility.wasAssociatedWith(processExecResource, wasAssociatedWithResource);
        Property isPartOf = rdfUtility.isPartOf(processExecResource, workflowResource);
        return processExecResource;

        //}
*/
      //  return null;
    }

}
