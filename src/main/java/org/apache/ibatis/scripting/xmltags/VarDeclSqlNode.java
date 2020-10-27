/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.scripting.xmltags;

/**
 * @author Frank D. Martinez [mnesarco]
 *
 * bind标签demo：https://www.cnblogs.com/bpjj/p/11880424.html
 *
 *
 */
public class VarDeclSqlNode implements SqlNode {

  private final String name;
  private final String expression;

  public VarDeclSqlNode(String var, String exp) {
    name = var;
    expression = exp;
  }

  @Override
  public boolean apply(DynamicContext context) {
    // Ognl计算表达式的结果，如果表达式用到了其他的属性，会从context.getBindings()里取属性
    final Object value = OgnlCache.getValue(expression, context.getBindings());
    // context又新增了一个属性对
    context.bind(name, value);
    return true;
  }

}
