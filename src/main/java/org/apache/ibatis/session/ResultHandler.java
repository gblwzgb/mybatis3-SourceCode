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
package org.apache.ibatis.session;

/**
 * @author Clinton Begin
 */
// 一行一行的处理返回值，用户可以自定义，在mapper方法的入参里，传入一个 ResultHandler 类型的参数，然后mapper出参是void的，就可以生效了。
// 业务上一般不会用这个，所以会有默认实现 DefaultResultHandler
public interface ResultHandler<T> {

  void handleResult(ResultContext<? extends T> resultContext);

}
