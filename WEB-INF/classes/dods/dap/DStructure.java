/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged.
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.dap;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A <code>DStructure</code> in DODS can hold <em>N</em> instances of any of the other datatypes,
 * including other structures.
 *
 * @version $Revision: 1.9 $
 * @author jehamby
 * @see BaseType
 * @see DConstructor
 */
public class DStructure extends DConstructor implements ClientIO {
  /**
   * The variables in this <code>DStructure</code>, stored in a <code>Vector</code> of <code>
   * BaseType</code> objects.
   */
  protected Vector vars;

  /** Constructs a new <code>DStructure</code>. */
  public DStructure() {
    this(null);
  }

  /**
   * Constructs a new <code>DStructure</code> with name <code>n</code>.
   *
   * @param n the name of the variable.
   */
  public DStructure(String n) {
    super(n);
    vars = new Vector<>();
  }

  /**
   * Returns a clone of this <code>DSequence</code>. A deep copy is performed on all data inside the
   * variable.
   *
   * @return a clone of this <code>DSequence</code>.
   */
  @Override
  public DStructure clone() {
    DStructure s = (DStructure) super.clone();
    s.vars = new Vector<>();
    for (int i = 0; i < vars.size(); i++) {
      BaseType bt = (BaseType) vars.elementAt(i);
      s.vars.addElement(bt.clone());
    }
    return s;
  }

  /**
   * Returns the DODS type name of the class instance as a <code>String</code>.
   *
   * @return the DODS type name of the class instance as a <code>String</code>.
   */
  @Override
  public String getTypeName() {
    return "Structure";
  }

  /**
   * Returns the number of variables contained in this object. For simple and vector type variables,
   * it always returns 1. To count the number of simple-type variable in the variable tree rooted at
   * this variable, set <code>leaves</code> to <code>true</code>.
   *
   * @param leaves If true, count all the simple types in the `tree' of variables rooted at this
   *     variable.
   * @return the number of contained variables.
   */
  @Override
  public int elementCount(boolean leaves) {
    if (!leaves) return vars.size();
    else {
      int count = 0;
      for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
        BaseType bt = (BaseType) e.nextElement();
        count += bt.elementCount(leaves);
      }
      return count;
    }
  }

  /**
   * Adds a variable to the container.
   *
   * @param v the variable to add.
   * @param part ignored for <code>DSequence</code>.
   */
  @Override
  public void addVariable(BaseType v, int part) {
    v.setParent(this);
    vars.addElement(v);
  }

  /**
   * Returns the named variable.
   *
   * @param name the name of the variable.
   * @return the named variable.
   * @exception NoSuchVariableException if the named variable does not exist in this container.
   */
  @Override
  public BaseType getVariable(String name) throws NoSuchVariableException {
    int dotIndex = name.indexOf('.');
    if (dotIndex != -1) { // name contains "."
      String aggregate = name.substring(0, dotIndex);
      String field = name.substring(dotIndex + 1);

      BaseType aggRef = getVariable(aggregate);
      if (aggRef instanceof DConstructor)
        return ((DConstructor) aggRef).getVariable(field); // recurse
      else
        ; // fall through to throw statement
    } else {
      for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
        BaseType v = (BaseType) e.nextElement();
        if (v.getName().equals(name)) return v;
      }
    }
    throw new NoSuchVariableException("DStructure: getVariable()");
  }

  /**
   * Gets the indexed variable. For a DStructure this returns the <code>BaseType</code> from the
   * <code>index</code>th column from the internal storage <code>Vector</code>.
   *
   * @param index the index of the variable in the <code>Vector</code> Vars.
   * @return the indexed variable.
   * @exception NoSuchVariableException if the named variable does not exist in this container.
   */
  @Override
  public BaseType getVar(int index) throws NoSuchVariableException {

    if (index < vars.size()) return ((BaseType) vars.elementAt(index));
    else throw new NoSuchVariableException("DStructure.getVariable(" + index + " - 1)");
  }

  /**
   * Return an Enumeration that can be used to iterate over the members of a Structure. This
   * implementation provides access to the elements of the Structure. Each Object returned by the
   * Enumeration can be cast to a BaseType.
   *
   * @return An Enumeration
   */
  @Override
  public Enumeration getVariables() {
    return vars.elements();
  }

  /**
   * Checks for internal consistency. For <code>DStructure</code>, verify that the variables have
   * unique names.
   *
   * @param all for complex constructor types, this flag indicates whether to check the semantics of
   *     the member variables, too.
   * @exception BadSemanticsException if semantics are bad, explains why.
   * @see BaseType#checkSemantics(boolean)
   */
  @Override
  public void checkSemantics(boolean all) throws BadSemanticsException {
    super.checkSemantics(all);

    Util.uniqueNames(vars, getName(), getTypeName());

    if (all) {
      for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
        BaseType bt = (BaseType) e.nextElement();
        bt.checkSemantics(true);
      }
    }
  }

  /**
   * Write the variable's declaration in a C-style syntax. This function is used to create textual
   * representation of the Data Descriptor Structure (DDS). See <em>The DODS User Manual</em> for
   * information about this structure.
   *
   * @param os The <code>PrintWriter</code> on which to print the declaration.
   * @param space Each line of the declaration will begin with the characters in this string.
   *     Usually used for leading spaces.
   * @param print_semi a boolean value indicating whether to print a semicolon at the end of the
   *     declaration.
   * @see BaseType#printDecl(PrintWriter, String, boolean)
   */
  @Override
  public void printDecl(PrintWriter os, String space, boolean print_semi, boolean constrained) {

    // BEWARE! Since printDecl()is (multiple) overloaded in BaseType and
    // all of the different signatures of printDecl() in BaseType lead to
    // one signature, we must be careful to override that SAME signature
    // here. That way all calls to printDecl() for this object lead to
    // this implementation.

    os.println(space + getTypeName() + " {");
    for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
      BaseType bt = (BaseType) e.nextElement();
      bt.printDecl(os, space + "    ", true, constrained);
    }
    os.print(space + "} " + getName());

    if (print_semi) os.println(";");
  }

  /**
   * Prints the value of the variable, with its declaration. This function is primarily intended for
   * debugging DODS applications and text-based clients such as geturl.
   *
   * @param os the <code>PrintWriter</code> on which to print the value.
   * @param space this value is passed to the <code>printDecl</code> method, and controls the
   *     leading spaces of the output.
   * @param print_decl_p a boolean value controlling whether the variable declaration is printed as
   *     well as the value.
   * @see BaseType#printVal(PrintWriter, String, boolean)
   */
  @Override
  public void printVal(PrintWriter os, String space, boolean print_decl_p) {
    if (print_decl_p) {
      printDecl(os, space, false);
      os.print(" = ");
    }

    os.print("{ ");
    for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
      BaseType bt = (BaseType) e.nextElement();
      bt.printVal(os, "", false);
      if (e.hasMoreElements()) os.print(", ");
    }
    os.print(" }");

    if (print_decl_p) os.println(";");
  }

  /**
   * Reads data from a <code>DataInputStream</code>. This method is only used on the client side of
   * the DODS client/server connection.
   *
   * @param source a <code>DataInputStream</code> to read from.
   * @param sv the <code>ServerVersion</code> returned by the server.
   * @param statusUI the <code>StatusUI</code> object to use for GUI updates and user cancellation
   *     notification (may be null).
   * @exception EOFException if EOF is found before the variable is completely deserialized.
   * @exception IOException thrown on any other InputStream exception.
   * @exception DataReadException if an unexpected value was read.
   * @see ClientIO#deserialize(DataInputStream, ServerVersion, StatusUI)
   */
  @Override
  public synchronized void deserialize(DataInputStream source, ServerVersion sv, StatusUI statusUI)
      throws IOException, EOFException, DataReadException {
    for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
      if (statusUI != null && statusUI.userCancelled())
        throw new DataReadException("User cancelled");
      ClientIO bt = (ClientIO) e.nextElement();
      bt.deserialize(source, sv, statusUI);
    }
  }

  /**
   * Writes data to a <code>DataOutputStream</code>. This method is used primarily by GUI clients
   * which need to download DODS data, manipulate it, and then re-save it as a binary file.
   *
   * @param sink a <code>DataOutputStream</code> to write to.
   * @exception IOException thrown on any <code>OutputStream</code> exception.
   */
  @Override
  public void externalize(DataOutputStream sink) throws IOException {
    for (Enumeration e = vars.elements(); e.hasMoreElements(); ) {
      ClientIO bt = (ClientIO) e.nextElement();
      bt.externalize(sink);
    }
  }
}
