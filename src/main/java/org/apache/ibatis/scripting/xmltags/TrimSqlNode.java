/**
 *    Copyright 2009-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 *
 * trim标签的用法参考：https://www.cnblogs.com/qiankun-site/p/5758924.html
 *
 * 先把匹配到的第一个前、后缀删了。然后插入一个指定的前、后缀。
 * （PS：这里千万不能理解成替换，即使没匹配到待override的前缀，也会插入）
 *
 * 使用这个，就不用再写where 1=1这样的sql了，可以直接用WhereSqlNode
 */
public class TrimSqlNode implements SqlNode {

  private final SqlNode contents;
  // 待插入的前缀
  private final String prefix;
  // 待插入的后缀
  private final String suffix;
  // 待删除的前缀，String中使用|分隔
  private final List<String> prefixesToOverride;
  // 待删除的后缀，String中使用|分隔
  private final List<String> suffixesToOverride;
  private final Configuration configuration;

  public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, String prefixesToOverride, String suffix, String suffixesToOverride) {
    this(configuration, contents, prefix, parseOverrides(prefixesToOverride), suffix, parseOverrides(suffixesToOverride));
  }

  protected TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, List<String> prefixesToOverride, String suffix, List<String> suffixesToOverride) {
    this.contents = contents;
    this.prefix = prefix;
    this.prefixesToOverride = prefixesToOverride;
    this.suffix = suffix;
    this.suffixesToOverride = suffixesToOverride;
    this.configuration = configuration;
  }

  @Override
  public boolean apply(DynamicContext context) {
    // 搞一个临时的拦截器，这个拦截器会暂存子SqlNode处理好的sql，不会append到总的sql中去
    FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
    // 子SqlNode处理，将处理好的sql，append到FilteredDynamicContext中
    boolean result = contents.apply(filteredDynamicContext);
    // trim过滤FilteredDynamicContext中暂存的sql，然后append到DynamicContext中的总sql中
    filteredDynamicContext.applyAll();
    return result;
  }

  // 将字符串，转成List，通过|分隔
  private static List<String> parseOverrides(String overrides) {
    if (overrides != null) {
      final StringTokenizer parser = new StringTokenizer(overrides, "|", false);
      final List<String> list = new ArrayList<String>(parser.countTokens());
      while (parser.hasMoreTokens()) {
        // 转成大写
        list.add(parser.nextToken().toUpperCase(Locale.ENGLISH));
      }
      return list;
    }
    return Collections.emptyList();
  }

  private class FilteredDynamicContext extends DynamicContext {
    private DynamicContext delegate;
    private boolean prefixApplied;
    private boolean suffixApplied;
    // 暂存子SqlNode处理完毕的内容
    private StringBuilder sqlBuffer;

    public FilteredDynamicContext(DynamicContext delegate) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefixApplied = false;
      this.suffixApplied = false;
      this.sqlBuffer = new StringBuilder();
    }

    public void applyAll() {
      // 去掉首尾的空白字符串
      sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
      // 待处理的SQL，全部转成大写
      String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
      if (trimmedUppercaseSql.length() > 0) {
        // 处理前缀
        applyPrefix(sqlBuffer, trimmedUppercaseSql);
        // 处理后缀
        applySuffix(sqlBuffer, trimmedUppercaseSql);
      }
      delegate.appendSql(sqlBuffer.toString());
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

    @Override
    public void appendSql(String sql) {
      // 在这里先将sql拦截下来，不要放入总的sql中。
      // 拦截下来的就是<trim>标签中的sql片段，等待filter处理
      sqlBuffer.append(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!prefixApplied) {
        prefixApplied = true;
        if (prefixesToOverride != null) {
          for (String toRemove : prefixesToOverride) {
            if (trimmedUppercaseSql.startsWith(toRemove)) {
              // 匹配到了，先删除
              sql.delete(0, toRemove.trim().length());
              // 不继续匹配了
              break;
            }
          }
        }
        // 设置了prefix，则替换
        if (prefix != null) {
          sql.insert(0, " ");
          sql.insert(0, prefix);
        }
      }
    }

    private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!suffixApplied) {
        suffixApplied = true;
        if (suffixesToOverride != null) {
          for (String toRemove : suffixesToOverride) {
            if (trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim())) {
              int start = sql.length() - toRemove.trim().length();
              int end = sql.length();
              // 匹配到了，先删除
              sql.delete(start, end);
              // 不继续匹配了
              break;
            }
          }
        }
        // 设置了suffix，则替换
        if (suffix != null) {
          sql.append(" ");
          sql.append(suffix);
        }
      }
    }

  }

}
