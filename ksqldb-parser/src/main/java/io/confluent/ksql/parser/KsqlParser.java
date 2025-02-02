/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.parser;

import com.google.errorprone.annotations.Immutable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.confluent.ksql.metastore.TypeRegistry;
import io.confluent.ksql.parser.SqlBaseParser.SingleStatementContext;
import io.confluent.ksql.parser.tree.Statement;
import java.util.List;
import java.util.Objects;

/**
 * A SQL parser.
 */
public interface KsqlParser {

  /**
   * Parse the supplied {@code sql} into a list of statements.
   *
   * @param sql the sql to parse.
   * @return the list of parsed statements.
   */
  List<ParsedStatement> parse(String sql);

  /**
   * Prepare the supplied {@code statement}.
   *
   * @param statement the statement to build
   * @param typeRegistry the type registry
   * @return the prepared statement.
   */
  PreparedStatement<?> prepare(ParsedStatement statement, TypeRegistry typeRegistry);

  final class ParsedStatement {
    private final String statementText;
    private final SingleStatementContext statement;

    private ParsedStatement(final String statementText, final SingleStatementContext statement) {
      this.statementText = Objects.requireNonNull(statementText, "statementText");
      this.statement = Objects.requireNonNull(statement, "statement");
    }

    public static ParsedStatement of(
        final String statementText,
        final SingleStatementContext statement
    ) {
      return new ParsedStatement(statementText, statement);
    }

    public String getStatementText() {
      return statementText;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public SingleStatementContext getStatement() {
      return statement;
    }
  }

  @Immutable
  final class PreparedStatement<T extends Statement> {

    private final String statementText;
    private final T statement;

    private PreparedStatement(final String statementText, final T statement) {
      this.statementText = Objects.requireNonNull(statementText, "statementText");
      this.statement = Objects.requireNonNull(statement, "statement");
    }

    public static <T extends Statement> PreparedStatement<T> of(
        final String statementText,
        final T statement
    ) {
      return new PreparedStatement<>(statementText, statement);
    }

    public String getStatementText() {
      return statementText;
    }

    public T getStatement() {
      return statement;
    }

    @Override
    public String toString() {
      return statementText;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final PreparedStatement<?> that = (PreparedStatement) o;
      return Objects.equals(this.statementText, that.statementText)
          && Objects.equals(this.statement, that.statement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(statementText, statement);
    }
  }
}
