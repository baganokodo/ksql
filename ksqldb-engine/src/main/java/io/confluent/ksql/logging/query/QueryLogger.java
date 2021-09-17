/*
 * Copyright 2021 Confluent Inc.
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * http://www.confluent.io/confluent-community-license
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.logging.query;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.confluent.ksql.engine.rewrite.QueryAnonymizer;
import io.confluent.ksql.parser.DefaultKsqlParser;
import io.confluent.ksql.parser.ParsingException;
import io.confluent.ksql.parser.SqlFormatter;
import io.confluent.ksql.parser.tree.Statement;
import io.confluent.ksql.util.KsqlConfig;
import io.confluent.ksql.util.QueryGuid;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public final class QueryLogger {
  private static final Logger logger = LogManager.getLogger(QueryLogger.class);
  private static QueryAnonymizingRewritePolicy rewritePolicy = null;

  private QueryLogger() {

  }

  @VisibleForTesting
  public static String getNamespace() {
    return rewritePolicy.getNamespace();
  }

  public static void addAppender(final Appender appender) {
    logger.addAppender(appender);
  }

  @SuppressFBWarnings(value = "MS_EXPOSE_REP")
  public static Logger getLogger() {
    return logger;
  }

  public static void configure(final KsqlConfig config) {
    rewritePolicy = new QueryAnonymizingRewritePolicy(config);
  }

  private static void log(final Level level, final Object message, final String query) {
    try {
      final String anonymizedQuery = rewritePolicy.getAnonymizer().anonymize(query);
      final QueryGuid queryGuids = rewritePolicy.buildGuids(query, anonymizedQuery);
      logger.log(level, buildPayload(message, anonymizedQuery, queryGuids));
    } catch (ParsingException e) {
      if (logger.isDebugEnabled()) {
        Logger.getRootLogger()
            .log(
                Level.DEBUG,
                String.format("Failed to parse a query in query logger, message: %s", message));
      }
    }
  }

  private static void log(final Level level, final Object message, final Statement query) {
    final String queryString = SqlFormatter.formatSql(query);
    final String anonymizedQuery = rewritePolicy.getAnonymizer().anonymize(queryString);
    final QueryGuid queryGuids = rewritePolicy.buildGuids(queryString, anonymizedQuery);
    logger.log(level, buildPayload(message, queryString, queryGuids));
  }

  private static QueryLoggerMessage buildPayload(final Object message, final String query, final QueryGuid guid) {
    return new QueryLoggerMessage(message, query, guid);
  }

  public static void debug(final Object message, final String query) {
    log(Level.DEBUG, message, query);
  }

  public static void debug(final Object message, final Statement query) {
    log(Level.DEBUG, message, query);
  }

  public static void info(final Object message, final String query) {
    log(Level.INFO, message, query);
  }

  public static void info(final Object message, final Statement query) {
    log(Level.INFO, message, query);
  }

  public static void warn(final Object message, final String query) {
    log(Level.WARN, message, query);
  }

  public static void warn(final Object message, final Statement query) {
    log(Level.WARN, message, query);
  }

  public static void error(final Object message, final String query) {
    log(Level.ERROR, message, query);
  }

  public static void error(final Object message, final Statement query) {
    log(Level.ERROR, message, query);
  }
}
