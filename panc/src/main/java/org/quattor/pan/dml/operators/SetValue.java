/*
 Copyright (c) 2006 Charles A. Loomis, Jr, Cedric Duprilot, and
 Centre National de la Recherche Scientifique (CNRS).

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 $HeadURL: https://svn.lal.in2p3.fr/LCG/QWG/panc/trunk/src/org/quattor/pan/dml/operators/SetValue.java $
 $Id: SetValue.java 3620 2008-08-21 14:36:32Z loomis $
 */

package org.quattor.pan.dml.operators;

import static org.quattor.pan.utils.MessageUtils.MSG_AUTO_VAR_CANNOT_BE_SET;
import static org.quattor.pan.utils.MessageUtils.MSG_INVALID_EXECUTE_METHOD_CALLED;
import static org.quattor.pan.utils.MessageUtils.MSG_INVALID_TERM;

import org.quattor.pan.dml.AbstractOperation;
import org.quattor.pan.dml.Operation;
import org.quattor.pan.dml.data.Element;
import org.quattor.pan.exceptions.CompilerError;
import org.quattor.pan.exceptions.EvaluationException;
import org.quattor.pan.exceptions.SyntaxException;
import org.quattor.pan.ttemplate.Context;
import org.quattor.pan.ttemplate.SourceRange;
import org.quattor.pan.utils.Term;
import org.quattor.pan.utils.TermFactory;

/**
 * Implements a special operation to allow a result to be assigned to a
 * variable. This is generated by the compiler, but is not explicitly visible in
 * the pan language.
 * 
 * @author loomis
 * 
 */
public class SetValue extends AbstractOperation {

	/**
	 * This array contains the names of 'automatic' variables. These names are
	 * reserved by the pan compiler and cannot be set directly by the user.
	 * Note, a couple of values can be set, like ARGV and ARGC. SELF is handled
	 * separately because the rules are more complicated.
	 */
	private static final String[] automaticVariables = new String[] {
			"OBJECT", "FUNCTION", "TEMPLATE" };

	protected String identifier;

	protected SetValue(SourceRange sourceRange, String identifier,
			Operation... operations) throws SyntaxException {
		super(sourceRange, operations);

		assert (identifier != null);
		this.identifier = identifier;

		// Ensure the name is valid.
		validName(identifier);

		// Ensure constant terms are valid.
		checkStaticIndexes(sourceRange, operations);
	}

	// Ensure that any constant indexes in the operation list are valid terms.
	protected static void checkStaticIndexes(SourceRange sourceRange,
			Operation... operations) throws SyntaxException {

		for (int i = 0; i < operations.length; i++) {
			if (operations[i] instanceof Element) {
				try {
					TermFactory.create((Element) operations[i]);
				} catch (EvaluationException ee) {
					throw SyntaxException.create(sourceRange, MSG_INVALID_TERM,
							i);
				}
			}
		}
	}

	/**
	 * A utility method to determine if the variable name collides with one of
	 * the reserved 'automatic' variables.
	 * 
	 * @param name
	 *            variable name to check
         * @throws SyntaxException when syntax error is detected
	 */
	protected void validName(String name) throws SyntaxException {
		for (String varName : automaticVariables) {
			if (varName.equals(name)) {
				throw SyntaxException.create(getSourceRange(),
						MSG_AUTO_VAR_CANNOT_BE_SET, varName);
			}
		}
	}

	public static SetValue getInstance(SourceRange sourceRange,
			String identifier, Operation... operations) throws SyntaxException {
		return createSubclass(sourceRange, identifier, operations);
	}

	public static SetValue getInstance(Variable v) throws SyntaxException {

		SourceRange sourceRange = v.getSourceRange();
		String identifier = v.identifier;
		Operation[] operations = v.getOperations();

		return createSubclass(sourceRange, identifier, operations);
	}

	private static SetValue createSubclass(SourceRange sourceRange,
			String identifier, Operation... operations) throws SyntaxException {
		SetValue result;
		if ("SELF".equals(identifier)) {
			result = new SetSelf(sourceRange, operations);
		} else {
			result = new SetValue(sourceRange, identifier, operations);
		}
		return result;
	}

	@Override
	public Element execute(Context context) {

		// This operation is intended to only be called from an Assign
		// operation. If this is called, then incorrect code was generated by
		// the compiler.
		throw CompilerError.create(MSG_INVALID_EXECUTE_METHOD_CALLED,
				"SetValue");
	}

	public Element execute(Context context, Element result) {

		// Create an array containing the terms for dereferencing.
		Term[] terms = null;
		try {
			terms = calculateTerms(context);
		} catch (EvaluationException e) {
			throw e.addExceptionInfo(sourceRange, context);
		}

		// Duplicate the result only if necessary. Within a DML block,
		// duplicating the element is only necessary if the value we are going
		// to set has parents. This is to avoid any possibility of creating
		// cyclic data structures. This requirement essentially comes down to
		// checking if nterms is positive.
		//
		// Also need to duplicate if this is a protected value. This is to make
		// sure that global variables copied into a local variable behave as
		// expected. (I.e. pulling out children
		//
		Element dupResult = result;
		if (result != null) {
			if (terms.length > 0 || result.isProtected()) {
				dupResult = result.duplicate();
			}
		}

		// Now set the value. May throw an exception if this is a global
		// variable.
		try {
			context.setLocalVariable(identifier, terms, dupResult);
		} catch (EvaluationException ee) {
			throw ee.addExceptionInfo(sourceRange, context);
		}

		// Push the result onto the data stack.
		return dupResult;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + identifier + ","
				+ ops.length + ")";
	}

}
