/* Generated By:JJTree: Do not edit this line. DCL_Floating.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=DCL_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.piax.common.dcl.parser;

public
@SuppressWarnings("all")
class DCL_Floating extends SimpleNode {
  public DCL_Floating(int id) {
    super(id);
  }

  public DCL_Floating(DCLParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(DCLParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=4548ad00cca849abc3964c3aa251685c (do not edit this line) */
