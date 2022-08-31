package it.unipr.ailab.jadescript.ui.syntaxhighlight;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;
import org.eclipse.xtext.ui.editor.utils.TextStyle;

public class JadescriptHighlightingConfiguration extends DefaultHighlightingConfiguration{
	
	public static final String PROCEDURE_CALL_IDENTIFIER = "ProcedureCallIdentifier";
    public static final String TYPE_REFERENCE = "TypeReference";
    public static final String BASIC_TYPE = "BasicType";
    
	@Override
	public void configure(IHighlightingConfigurationAcceptor acceptor) {
		super.configure(acceptor);

//        acceptor.acceptDefaultHighlighting(PROCEDURE_CALL_IDENTIFIER, "Procedure call identifier", procedureCallId());
        acceptor.acceptDefaultHighlighting(TYPE_REFERENCE, "Type reference", typeReference());
        acceptor.acceptDefaultHighlighting(BASIC_TYPE, "Primitive type name", basicType());
	}
	
	protected TextStyle basicType() {
		TextStyle result = keywordTextStyle().copy();
		result.setStyle(SWT.ITALIC);
		return result;
	}

	protected  TextStyle typeReference() {
		TextStyle textStyle = defaultTextStyle().copy();
        textStyle.setColor(new RGB(0x24, 0x9A, 0xA7));
        return textStyle;
	}


	
	
//	protected TextStyle procedureCallId() {
//		TextStyle textStyle = defaultTextStyle().copy();
//        textStyle.setColor(new RGB(0xEE, 0xA2, 0x31));
//        textStyle.setStyle(SWT.ITALIC);
//        return textStyle;
//	}

}