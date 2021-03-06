/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.cmdline.matcher;

import org.crsh.cmdline.ArgumentDescriptor;
import org.crsh.cmdline.OptionDescriptor;
import org.crsh.cmdline.binding.MethodArgumentBinding;
import org.crsh.cmdline.MethodDescriptor;
import org.crsh.cmdline.ParameterDescriptor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodMatch<T> extends CommandMatch<T, MethodDescriptor<T>, MethodArgumentBinding> {

  /** . */
  private final MethodDescriptor<T> descriptor;

  /** . */
  private final ClassMatch<T> owner;

  /** . */
  private final boolean implicit;

  public MethodMatch(
    ClassMatch<T> owner,
    MethodDescriptor<T> descriptor,
    boolean implicit,
    List<OptionMatch<MethodArgumentBinding>> optionMatches,
    List<ArgumentMatch<MethodArgumentBinding>> argumentMatches,
    String rest) {
    super(optionMatches, argumentMatches, rest);

    //
    this.owner = owner;
    this.descriptor = descriptor;
    this.implicit = implicit;
  }

  public boolean isImplicit() {
    return implicit;
  }

  @Override
  public MethodDescriptor<T> getDescriptor() {
    return descriptor;
  }

  public ClassMatch<T> getOwner() {
    return owner;
  }

  @Override
  public void printMan(Appendable writer) throws IOException {
    if (implicit) {
      getOwner().printMan(writer);
    } else {
      descriptor.printMan(writer);
    }
  }

  @Override
  public void printUsage(Appendable writer) throws IOException {
    if (implicit) {
      getOwner().printUsage(writer);
    } else {
      descriptor.printUsage(writer);
    }
  }

  @Override
  public Set<ParameterDescriptor<?>> getParameters() {
    Set<ParameterDescriptor<?>> unused = new HashSet<ParameterDescriptor<?>>();
    unused.addAll(descriptor.getArguments());
    unused.addAll(descriptor.getOptions());
    unused.addAll(owner.getDescriptor().getOptions());
    return unused;
  }

  @Override
  public List<ParameterMatch<?, ?>> getParameterMatches() {
    List<ParameterMatch<?, ?>> matches = new ArrayList<ParameterMatch<?, ?>>();
    matches.addAll(getOptionMatches());
    matches.addAll(getArgumentMatches());
    matches.addAll(owner.getOptionMatches());
    return matches;
  }

  @Override
  protected Object doInvoke(Resolver context, T command, Map<ParameterDescriptor<?>, Object> values) throws CmdInvocationException, CmdSyntaxException {

    // Prepare invocation
    MethodDescriptor<T> descriptor = getDescriptor();
    Method m = descriptor.getMethod();
    Class<?>[] parameterTypes = m.getParameterTypes();
    Object[] mArgs = new Object[parameterTypes.length];
    for (int i = 0;i < mArgs.length;i++) {
      ParameterDescriptor<MethodArgumentBinding> parameter = descriptor.getParameter(i);

      //
      Class<?> parameterType = parameterTypes[i];

      //
      Object v;
      if (parameter == null) {
        // Attempt to obtain from invocation context
        v = context.resolve(parameterType);
      } else {
        v = values.get(parameter);
      }

      //
      if (v == null) {
        if (parameterType.isPrimitive() || parameter.isRequired()) {
          if (parameter instanceof ArgumentDescriptor) {
            ArgumentDescriptor<?> argument = (ArgumentDescriptor<?>)parameter;
            throw new CmdSyntaxException("Missing argument " + argument.getName());
          } else {
            OptionDescriptor<?> option = (OptionDescriptor<?>)parameter;
            throw new CmdSyntaxException("Missing option " + option.getNames());
          }
        }
      }

      //
      mArgs[i] = v;
    }

    // First configure command
    owner.doInvoke(context, command, values);

    // Perform method invocation
    try {
      return m.invoke(command, mArgs);
    }
    catch (InvocationTargetException e) {
      Throwable t = e.getTargetException();
      if (t instanceof Error) {
        throw (Error)t;
      } else {
        throw new CmdInvocationException(t);
      }
    }
    catch (IllegalAccessException t) {
      throw new CmdInvocationException(t);
    }
  }
}
