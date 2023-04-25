package jadescript.content.onto.basic;


import jadescript.content.JadescriptPredicate;

public class InvalidNativeOperationInvocation implements JadescriptPredicate {

    private String reason;
    private String operationName;


    public InvalidNativeOperationInvocation() {
        this.reason = "";
        this.operationName = "";
    }


    public InvalidNativeOperationInvocation(
        String reason,
        String operationName
    ) {
        this.reason = reason;
        this.operationName = operationName;
    }


    @Override
    public String toString() {
        return "InvalidNativeOperationInvocation(" +
            "reason='" + reason + "', operationName='" + operationName + "')";
    }


    public String getOperationName() {
        return operationName;
    }


    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }


    public String getReason() {
        return reason;
    }


    public void setReason(String reason) {
        this.reason = reason;
    }


    @Override
    public jadescript.content.onto.Ontology __getDeclaringOntology() {
        return jadescript.content.onto.Ontology.getInstance();
    }



    public jadescript.content.onto.Ontology __metadata_jadescript_content_onto_basic_InvalidNativeOperationName(){
        return null;
    }
}
