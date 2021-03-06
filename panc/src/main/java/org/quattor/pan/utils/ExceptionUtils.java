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

 $HeadURL: https://svn.lal.in2p3.fr/LCG/QWG/panc/trunk/src/org/quattor/pan/utils/ExceptionUtils.java $
 $Id: ExceptionUtils.java 3660 2008-09-04 06:33:19Z loomis $
 */

package org.quattor.pan.utils;

import java.util.concurrent.ExecutionException;

import org.quattor.pan.exceptions.EvaluationException;
import org.quattor.pan.exceptions.SyntaxException;

/**
 * Launders the throwables, errors, and exceptions that can be returned from a
 * <code>Future</code>.
 * 
 * @author loomis
 * 
 */
public class ExceptionUtils {

	private ExceptionUtils() {
	}

	public static RuntimeException launder(ExecutionException exception) {

		Throwable t = exception.getCause();
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		} else if (t instanceof SyntaxException) {
			SyntaxException se = (SyntaxException) t;
			EvaluationException ee = new EvaluationException(se.getMessage());
			ee.initCause(se);
			throw ee;
		} else if (t instanceof Error) {
			throw (Error) t;
		} else {
			throw new EvaluationException("unexpected throwable encountered: "
					+ t);
		}
	}

}
