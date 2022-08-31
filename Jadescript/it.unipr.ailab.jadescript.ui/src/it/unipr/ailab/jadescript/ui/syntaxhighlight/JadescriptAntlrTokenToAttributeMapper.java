package it.unipr.ailab.jadescript.ui.syntaxhighlight;

import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultAntlrTokenToAttributeIdMapper;

public class JadescriptAntlrTokenToAttributeMapper extends DefaultAntlrTokenToAttributeIdMapper
{

	@Override
	protected String calculateId(String tokenName, int tokenType) {
		if(tokenName.equals("RULE_TIMESTAMP")) {
			return JadescriptHighlightingConfiguration.NUMBER_ID;
		}
		
		if(tokenName.equals("RULE_DURATION")) {
			return JadescriptHighlightingConfiguration.NUMBER_ID;
		}
		
		if(tokenName.equals("RULE_STRINGSIMPLE")) {
			return JadescriptHighlightingConfiguration.STRING_ID;
		}
		
		if(tokenName.equals("RULE_STRINGSTART")) {
			return JadescriptHighlightingConfiguration.STRING_ID;
		}
		
		if(tokenName.equals("RULE_STRINGMIDDLE")) {
			return JadescriptHighlightingConfiguration.STRING_ID;
		}
		
		if(tokenName.equals("RULE_STRINGEND")) {
			return JadescriptHighlightingConfiguration.STRING_ID;
		}
		
		return super.calculateId(tokenName, tokenType);
	}

}
