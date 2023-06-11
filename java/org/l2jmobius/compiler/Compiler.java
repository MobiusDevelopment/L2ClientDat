/*
 * This file is part of the L2ClientDat project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.compiler;

import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class Compiler
{
	private static final Logger LOGGER = Logger.getLogger(Compiler.class.getName());
	
	private static final JavaCompiler JAVAC = ToolProvider.getSystemJavaCompiler();
	
	private final DiagnosticListener<JavaFileObject> _listener;
	private final StandardJavaFileManager _fileManager;
	private final MemoryClassLoader _memClassLoader;
	private final MemoryJavaFileManager _memFileManager;
	
	public Compiler()
	{
		_listener = new DefaultDiagnosticListener();
		_fileManager = JAVAC.getStandardFileManager(_listener, Locale.getDefault(), Charset.defaultCharset());
		_memClassLoader = new MemoryClassLoader();
		_memFileManager = new MemoryJavaFileManager(_fileManager, _memClassLoader);
	}
	
	public boolean compile(File... files)
	{
		final List<String> options = new ArrayList<>();
		// options.add("-Xlint:all");
		options.add("-source");
		options.add("1.8");
		options.add("-g");
		
		final JavaCompiler.CompilationTask compile = Compiler.JAVAC.getTask(new StringWriter(), _memFileManager, _listener, options, null, _fileManager.getJavaFileObjects(files));
		return compile.call();
	}
	
	public boolean compile(Collection<File> files)
	{
		return compile(files.toArray(new File[0]));
	}
	
	public MemoryClassLoader getClassLoader()
	{
		return _memClassLoader;
	}
	
	private static class DefaultDiagnosticListener implements DiagnosticListener<JavaFileObject>
	{
		@Override
		public void report(Diagnostic<? extends JavaFileObject> diagnostic)
		{
			LOGGER.log(Level.WARNING, (((JavaFileObject) diagnostic.getSource()).getName() + ((diagnostic.getPosition() == -1L) ? "" : (":" + diagnostic.getLineNumber() + "," + diagnostic.getColumnNumber())) + ": " + diagnostic.getMessage(Locale.getDefault())));
		}
	}
}
