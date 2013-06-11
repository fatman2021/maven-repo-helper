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
 * @author Damien Raude-Morvan <drazzib@debian.org>
 */
public class RepositoryReportHTMLWriter implements RepositoryReportWriter {

    /**
     * Current level of item
     */
    private int itemLevel = 1;

    public void printSectionStart(String string) {
        String display = string + ":";

        System.out.println("<h2>");
        System.out.println(display);
        System.out.println("</h2>");
        System.out.println("<ul>");
    }

    public void printSectionEnd() {
        System.out.println("</ul>");
    }

    public void printItem(String string) {
        if (itemLevel > 1) {
            System.out.print("<ul>");
        }
        System.out.print("<li>");
        System.out.print(string);
        itemLevel++;
    }

    public void endItem() {
        itemLevel--;
        System.out.println("</li>");
        if (itemLevel > 1) {
            System.out.print("</ul>");
        }
    }

    public void printStart() {
        System.out.println("<html>");
        System.out.println("<header>");
        System.out.println("<title>Debian Maven repository QA</title>");
        System.out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
        System.out.println("</header>");
        System.out.println("<body>");
        System.out.println("<h1>Debian Maven repository QA</h1>");
    }

    public void printEnd() {
        System.out.println("</body>");
        System.out.println("</html>");

    }

}
