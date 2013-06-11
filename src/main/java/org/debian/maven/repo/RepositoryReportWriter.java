/*
 * Copyright 2011 Damien Raude-Morvan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.debian.maven.repo;

/**
 * Interface of any check's output.
 *
 * @author Damien Raude-Morvan <drazzib@debian.org>
 */
public interface RepositoryReportWriter {

    /**
     * Print start part of any Item
     *
     * @param string content of message to print
     */
    void printItem(String string);

    /**
     * Print end part of any Item
     */
    void endItem();

    /**
     * Print start of a report section
     *
     * @param string content of message to print
     */
    void printSectionStart(String string);

    /**
     * Print end of a report section
     */
    void printSectionEnd();

    /**
     * Print end footer of the report
     */
    void printEnd();

    /**
     * Print start header of the report
     */
    void printStart();

}
