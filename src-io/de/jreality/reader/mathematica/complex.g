//**************************************************
// * Mathematica Parser
// */


header {
/**
* this code is generated by ANTLR from the 'mathematica.g'-file
* @author Bernd Gonska
* @version 1.0
*/
package de.jreality.test.reader.mathematica;
import java.awt.*;
import java.util.*;
import de.jreality.geometry.*;
import de.jreality.math.*;
import de.jreality.scene.data.*;
import de.jreality.scene.*;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.*;
import de.jreality.util.LoggingSystem;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.util.Vector;
import de.jtem.mfc.field.Complex;
}

class ComplexListParser extends Parser;
options {
	k = 2;							// two token lookahead
}

{
/**
* was es tut
* 
*
*/
	// initialisierung und java variablen
	Vector dimensions= new Vector(); // (int)
	boolean firstDimCount=true;
	boolean failure=false;
	
	// depth ist die aktuelle Tiefe in der Unterliste (ganz unten ist es gleich der Dimension)
	// depth 0 : in der obersten Liste
	// depth max : in den Blaettern die hoffentlich nur complexe Zahlen enthalten
	// depth max ist dimenson-1

	private Vector getDim(Vector dimLength , Vector data){
		// rekursiv
		if (data==null) return null;// FehlerFall
		dimLength.add(data.size());
		try{
			Complex c=(Complex)data.get(0); // ist das naechste eine Complexe Zahl?
			return dimLength;				// dann: RekursionsEnde
		}
		catch(Exception e)
		{
		 //gehe davon aus das es ein Vector ist
		 return getDim(dimLength, (Vector)data.get(0));
		}
	}
	
	// dim=0 -> erste Tiefe
	// informatisch zaehlen
	private boolean checkSize(int dim, Vector dimLength, Vector data){
		// rekursiv
		if (data==null) return false;				// FehlerFall
		int len=data.size();						// Laenge des aktuellen Vectors
		if (dimLength.size()<=dim) return false;	// falsche Dimension
		if (len!=((Integer)dimLength.get(dim)).intValue())	return false; // falsche Laenge
		if (dim==dimLength.size()-1){	// RekursionsEnde:
			try{
				Complex c;
				for (int i=0;i<len;i++)
					c=(Complex)data.get(i);// Fehler falls keine Complexe Zahl
			}
			catch(Exception e){return false;}
			return true;
		}
		for(int i=0;i<len;i++){
			if (checkSize(dim+1,dimLength,(Vector)data.get(i))){}
			else return false;	// Fehler aus tieferer rekursion weitergeben 
		}
		return true; // alles ok gewesen
	}
	
	private boolean checkDim(Vector data){
		if (data==null) return false;
		Vector dims= new Vector();
		dims= getDim(dims,data);
		if (dims==null) return false;
		return checkSize(0,dims,data);
	}


	
	
/**
* konstructs a parser who can translate a
* mathematica-file to a corresponding 
* multidimensional Array of complex numbers
* @param    see superclass
* example: MathematicaParser p=
*	    new MathematicaParser(new MathematicaLexer(
*	     new FileReader(new File("file.m"))));
*/
}

// ------------------------------------------------------------------------------
// -------------------------------- Parser --------------------------------------
// ------------------------------------------------------------------------------

/**
* starts the parsing Process
* @param none 	sourcefile set by creating the object
* @returns Vector of Vector of..	 which holds the ComplexNumbers
*/
start returns [Vector data]
{data=new Vector();}
	:
	   (data=arrayThing[0])?
	  {
	  	if (!checkDim(data)) data=null;
	  }
	;


private 
arrayThing [int depth ] returns [Vector innerData]
{
Complex c=new Complex();
innerData=new Vector();
Vector nextArray=new Vector();
int childCount=0;
}
		:OPEN_BRACE
		(
		 	c= complexThing 	{ childCount++;	 innerData.add(c);	} 
			( COLLON
			  c= complexThing	{ childCount++;	 innerData.add(c);	} 	 
			)*
		|
			nextArray =arrayThing[depth+1] 		{ childCount++;	 innerData.add(nextArray);	} 
			( COLLON
			  nextArray =arrayThing[depth+1]	{ childCount++;	 innerData.add(nextArray);	} 
			)*
		)?
		CLOSE_BRACE
		;
	
// -------------------------------------------------- Zahlen -------------------------------------------

private
doubleThing returns[double d]
// liest ein double aus
	{d=0; double e=0; String sig="";}
    : (PLUS | MINUS {sig="-";} )?
    ( s:INTEGER_THING 
    		{d=Double.parseDouble(sig + s.getText());}
      (DOT
      	(s2:INTEGER_THING 
    		{ d=Double.parseDouble(sig + s.getText()+ "." + s2.getText());}
         )?
      )?
	| DOT s3:INTEGER_THING 
			{d=Double.parseDouble(sig + "0." + s3.getText());}
    )
    (e=exponentThing {d=d*Math.pow(10,e);})?
    ;
    
private 
exponentThing returns[int e]
// liest den exponenten fuer double_thing
{e=0; String sig="";}
    : STAR HAT 
    (PLUS | MINUS {sig="-";} )?
     s:INTEGER_THING
     	{e=Integer.parseInt(sig + s.getText() );}
	;
	
private 
complexThing returns[Complex c]
{double temp1=0;
 double temp2=0;
 int sign=1;
 int sign2=1;
 double re=0;
 double im=0;
 c=new Complex();
 c.setRe(0);
 c.setIm(0);}
	: 	
	( PLUS{sign=1;}|MINUS{sign=-1;})?
	(
		 "i"							{re=0;im=sign*1;} 
	 	|
	 	 temp1=doubleThing 				{re=sign*temp1; im=0;}
	 	 (
	 	 	"i"							{re=0; im=sign*temp1;}
	 	 	|
	 	 	(PLUS						{sign2=1;} 
	 	 	|MINUS						{sign2=-1;}		)?
	 	 	(temp2=doubleThing "i"		{im=sign2*temp2;}
	 	 	 | "i"						{im=sign2*1;}
	 	 	)
	 	 	
	 	 )?
	)
	{c.setRe(re); c.setIm(im); }
	;
	
	
	
/** ************************************************************************
*   ******************* The Mathematica Lexer ******************************
*   ************************************************************************
* this class is only for class MathematicaParser
*/

class ComplexListLexer extends Lexer;
options {
	charVocabulary = '\3'..'\377';
	k=2;
	testLiterals=false;
}
	/** Terminal Symbols */
OPEN_BRACE:		'{';
CLOSE_BRACE:	'}';
BACKS:			'\\';
SLASH:			'/';
MINUS:			'-';
PLUS:			'+';
DOT:			'.';
HAT:			'^';
STAR:			'*';
DDOT: 			':';
COLLON:			',';

ID
options {
	paraphrase = "an identifier";
	testLiterals=true;
}
	:	('a'..'z'|'A'..'Z'|'_') (ID_LETTER)*
	;

private 
ID_LETTER:
	('a'..'z'|'A'..'Z'|'_'|'0'..'9')
	;
	
INTEGER_THING
	: (DIGIT)+
	;
		
private
DIGIT:
	('0'..'9')
	;
	
STRING:
		'"'! (ESC | ~('"'|'\\'))* '"'!
	;
private
ESC:
		'\\'! ('\\' | '"')
	;

WS_:
		( ' '
		| '\t'
		| '\f'
		// handle newlines
		|	(options {
					generateAmbigWarnings=false;
				}
		: "\r\n"	// Evil DOS
			| '\r'		// MacINTosh
			| '\n'		// Unix (the right way)
			{newline(); } )	
		)+ { $setType(Token.SKIP); }
;