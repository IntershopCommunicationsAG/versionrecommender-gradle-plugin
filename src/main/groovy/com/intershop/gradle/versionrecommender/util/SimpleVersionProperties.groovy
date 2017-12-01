/*
 * Copyright 2015 Intershop Communications AG.
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
 *  limitations under the License.
 */
package com.intershop.gradle.versionrecommender.util

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode

/**
 * Override Java standard properties.
 * <ul>
 *     <li>The format of the file will be stored, so that comments are still available after an store of the changed content.</li>
 *     <li>Onlny an equals sign splitts the key and value.</li>
 * </ul>
 *
 */
@CompileStatic
class SimpleVersionProperties extends Properties {

    private static final long serialVersionUID = 1L

    /**
     * These are the hexadecimal digits.
     */
    private static final char[] hexDigit = [ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' ] as char[]

    /**
     * The equal sign character.
     */
    static final Character CHAR_EQUAL_SIGN = '='

    /**
     * This member holds the lines of the properties file.
     */
    private List<Line> lines = []

    SimpleVersionProperties() { }

    SimpleVersionProperties(File propertiesFile) throws IOException {
        try {
            load(new FileReader(propertiesFile))
        }catch (Exception ex) {  }
    }

    /**
     * This method checks whether the given line continues on the next one (line
     * continuation with "\\").
     *
     * @param aLine The line to be checked.
     *
     * @return <CODE>true</CODE> if the line continues on the next line.
     */
    private static boolean continueLine(String s) {
        int i = 0

        for(int j = s.length() - 1; j >= 0 && s.charAt(j--) == ('\\' as char);) {
            i++
        }

        return i % 2 == 1
    }


    /**
     * Reads a property list (key and element pairs) from the input
     * byte stream. The input stream is in a simple line-oriented
     * format as specified in
     * {@link #load(java.io.Reader) load(Reader)} and is assumed to use
     * the ISO 8859-1 character encoding; that is each byte is one Latin1
     * character. Characters not in Latin1, and certain special characters,
     * are represented in keys and elements using Unicode escapes as defined in
     * section 3.3 of
     * <cite>The Java&trade; Language Specification</cite>.
     * <p>
     * The specified stream remains open after this method returns.
     *
     * @param      inStream   the input stream.
     * @exception  IOException  if an error occurred when reading from the
     *             input stream.
     * @throws     IllegalArgumentException if the input stream contains a
     *             malformed Unicode escape sequence.
     * @since 1.2
     */
    synchronized void load(InputStream inStream) throws IOException {
        load(new InputStreamReader(inStream))
    }

    /**
     * This method load this properties from the given input stream.
     *
     * @param anInputStream The stream to read the properties from.
     *
     * @exception java.io.IOException if the stream could not be read.
     */

    @Override
    synchronized void load(Reader originalReader) throws IOException {
        // Build reader based on input stream.
        BufferedReader reader = new BufferedReader(originalReader)

        // Repeat till we're done.
        while (true) {
            // Get line.
            String line = reader.readLine()

            // No line - exit.
            if (line == null) {
                return
            }
            // Is line empty?
            if (line.trim().length() <= 0) {
                // Yes, empty line.
                lines.add(new EmptyLine())
            }
            else {
                // Not empty, get first character.
                char c = line.charAt(0)

                // Is line commented out?
                if (c == ('#' as char) || c == ('!' as char)) {
                    // Is a property included? (Only single Line properties will
                    // be supported.)

                    String delimiter = CHAR_EQUAL_SIGN.toString()
                    int pos = line.indexOf(delimiter)
                    if (pos > 1) {
                        int startText = 1
                        char p = line.charAt(startText)
                        while((p == ('#' as char) || c == ('!' as char)) && startText < pos) {
                            startText++
                            p = line.charAt(startText)
                        }
                        String value = line.length() > pos + 1 ? line.substring(pos + 1) : ""
                        CommentPropertyLine cpl = new CommentPropertyLine(line.substring(0, startText).trim(),
                                loadConvert(line.substring(startText, pos).trim()), loadConvert(value.trim()))
                        lines.add(cpl)
                    } else {
                        // Yes, comment line.
                        lines.add(new CommentLine(line))
                    }
                } else {
                    // Property line.
                    String s1
                    String s2

                    // Process line continuation.
                    for(; continueLine(line); line = new String(s2 + s1)) {
                        s1 = reader.readLine()
                        if (s1 == null)
                            s1 = new String("")
                        s2 = line.substring(0, line.length() - 1)
                        int k = 0
                        for(k = 0; k < s1.length(); k++)
                            if (" \t\r\n\f".indexOf(s1.charAt(k).toString()) == -1)
                                break

                        s1 = s1.substring(k, s1.length())
                    }

                    int i = line.length()
                    int j = 0

                    for(j = 0; j < i; j++)
                        if (" \t\r\n\f".indexOf(line.charAt(j).toString()) == -1)
                            break

                    int l
                    for(l = j; l < i; l++) {
                        char c1 = line.charAt(l)
                        if (c1 == '\\') {
                            l++
                            continue
                        }
                        if ("= \t\r\n\f".indexOf(c1.toString()) != -1)
                            break
                    }

                    int i1
                    for(i1 = l; i1 < i; i1++) {
                        if (" \t\r\n\f".indexOf(line.charAt(i1).toString()) == -1)
                            break
                    }

                    if ((i1 < i) && (CHAR_EQUAL_SIGN == line.charAt(i1)))
                        i1++


                    for(; i1 < i; i1++) {
                        if (" \t\r\n\f".indexOf(line.charAt(i1).toString()) == -1)
                            break
                    }

                    // Get key/value pair.
                    String key = line.substring(j, l)
                    String value = l >= i ? "" : line.substring(i1, i)

                    key = loadConvert(key)
                    value = loadConvert(value)
                    if(super.containsKey(key)) {
                        for(int sl = 0; sl < lines.size(); sl++) {
                            Line pline = lines.get(sl)
                            if (pline instanceof PropertyLine) {
                                PropertyLine pl = (PropertyLine)pline
                                if (pl.key.equals(key))
                                    lines.set(sl, new CommentPropertyLine("#", pl.key, pl.value))
                            }
                        }
                    }
                    super.put(key, value)
                    lines.add(new PropertyLine(key, value))
                }
            }
        }
    }

    /**
     * This method does the conversion of strings during loading of the
     * properties. This means replacing unicode literals by their original form.
     *
     * @param aString The string to be converted.
     *
     * @return The converted string.
     */
    @TypeChecked(TypeCheckingMode.SKIP)
    private static String loadConvert(String aString) {
        // Initialization.
        int length = aString.length()
        StringBuffer result = new StringBuffer(length)

        // Process each character.
        for(int j = 0; j < length;) {
            // Get character at current position.
            char c = aString.charAt(j++)

            // Is it a backslash?
            if (c == ('\\' as char) && (j + 1) < length) {
                // Get next character.
                c = aString.charAt(j++)

                // Unicode literal?
                if (c == ('u' as char)) {
                    int k = 0
                    for(int l = 0; l < 4; l++) {
                        // Depending on the character:
                        c = aString.charAt(j++)
                        switch (c) {
                            case {(it >= '0') && (it <= '9')}:
                                k = ((k << 4) + c) - ('0' as char)
                                break
                            case {(it >= 'a') && (it <= 'f')}:
                                k = ((k << 4) + 10 + c) - ('a' as char)
                                break
                            case {(it >= 'A') && (it <= 'F')}:
                                k = ((k << 4) + 10 + c) - ('A' as char)
                                break
                            default:
                                throw new IllegalArgumentException("Malformed \\uxxxx encoding.")
                                break
                        }
                    }
                    result.append((char)k)
                }
                else {
                    switch (c) {
                        case 't':
                            result.append('\t')
                            break
                        case 'r':
                            result.append('\r')
                            break
                        case 'n':
                            result.append('\n')
                            break
                        case 'f':
                            result.append('\f')
                            break
                        case '\\':
                            result.append('\\')
                            break
                        default:
                            result.append(c)
                            break
                    }
                }
            }
            else {
                result.append(c)
            }
        }
        return result.toString()
    }

    /**
     * This method sets a property with a given name to the given value.
     *
     * @param aKey The key of the property to set.
     *
     * @param aValue The value to set for the property.
     *
     * @return The previous value for the key.
     */
    synchronized Object put(String aKey, String aValue) {
        // Call super.
        Object result = super.put(aKey, aValue)

        PropertyLine searchResult = null

        lines.each {Line line ->
            if (line instanceof PropertyLine) {
                PropertyLine propLine = (PropertyLine)line

                if (propLine.key.equals(aKey)) {
                    // Key found
                    propLine.value = aValue
                    if (line instanceof CommentPropertyLine) {
                        CommentPropertyLine commentpropLine = (CommentPropertyLine)line
                        if (searchResult == null && !(searchResult instanceof PropertyLine)) {
                            searchResult = commentpropLine
                        }
                    }
                    else {
                        searchResult = propLine
                    }
                }
            }
        }

        if (searchResult != null && searchResult instanceof CommentPropertyLine) {
            ((CommentPropertyLine)searchResult).comment = ""
        }
        else if (searchResult == null) {
            lines.add(new PropertyLine(aKey, aValue))
        }

        // Return object returned from super.
        return result
    }

    /**
     * This method sets the property with the given key to the given value.
     *
     * @param aKey The key of the property to set.
     *
     * @param aValue The value to set for the property.
     *
     * @return The previous value for the key.
     */

    @Override
    synchronized Object setProperty(String aKey, String aValue) {
        return put(aKey, aValue)
    }

    /**
     * use custom {@link #put(String, String)} method also for other types
     */
    @Override
    synchronized Object put(Object aKey, Object aValue) {
        if (aValue != null)
            return put(aKey.toString(), aValue.toString())
        else
            return remove(aKey)
    }

    /**
     * This method sets a new property with the given key to the given value
     * with an comment.
     *
     * @param aKey The key of the property to set.
     *
     * @param aValue The value to set for the property.
     *
     * @param Array of comments.
     *
     * @return The previous value for the key.
     */

    synchronized Object setProperty(String aKey, String aValue, String[] comment) {
        Object result = setProperty(aKey, aValue)
        addComment(aKey, aValue, comment, true)
        return result
    }

    void addComment(String aKey, String[] comment) {
        addComment(aKey, null, comment, false)
    }

    void addComment(String aKey, String aValue, String[] comment, boolean changeValue) {
        boolean changed = false
        for(int i = 0; i < lines.size(); i++) {
            changed = changed || changeLine(lines, aKey, aValue, i, comment, changeValue)
        }

        if (!changed) {
            for(int j = 0; j < comment.length; j++) {
                lines.add(new CommentLine("# " + comment[j]))
            }

            lines.add(new CommentPropertyLine("#", aKey, ""))
        }
    }

    private boolean changeLine(List<Line> linesVector, String aKey, String aValue, int pos, String[] comment, boolean changeValue) {
        Line line = linesVector.get(pos)

        // Is it a property line?
        if (line instanceof PropertyLine) {
            PropertyLine propLine = (PropertyLine)line
            if (propLine.key.equals(aKey)) {
                // Key found
                if (comment != null && comment.length > 0) {
                    boolean addComment = true
                    if (pos - comment.length > 0) {
                        for(int i = pos - comment.length; i < pos; ++i) {
                            if (linesVector.get(i) instanceof CommentLine) {
                                CommentLine cl = (CommentLine)linesVector.get(i)
                                addComment = addComment && !cl.comment.equals("# " + comment[i - pos + comment.length])
                            }
                        }
                    }
                    if (addComment) {
                        for(int i = 0; i < comment.length; i++) {
                            linesVector.add(pos + i, new CommentLine("# " + comment[i]))
                        }
                    }
                }
                if (changeValue) {
                    propLine.value = aValue
                }
                return true
            }
        }
        return false
    }

    void store(File outputFile) throws IOException {
        try {
            Writer w = new FileWriter(outputFile)
            store(w)
        } catch (Exception ex) {
            ex.printStackTrace()
        }
    }

    /**
     * This method save this properties to the given output stream.
     *
     * @param anOutputStream The stream to save the properties to.
     *
     * @param aHeader A string to be written into the header.
     *
     * @exception java.io.IOException if the stream could not be written.
     */
    synchronized void store(Writer originalWriter, String... aHeader) throws IOException
    {
        BufferedWriter writer = new BufferedWriter(originalWriter)

        // Do we have a header to write?
        if (aHeader != null && aHeader.length > 0) {
            int pos = -1
            for(int i = 0; i < lines.size(); ++i) {
                if (lines.get(i) instanceof CommentLine) {
                    CommentLine cl = (CommentLine)lines.get(i)
                    if (cl.comment.equals("# " + aHeader[0])) {
                        pos = i
                    }
                    break
                }
            }

            if(pos < 0) {
                writeln(writer, "# " + aHeader[0])
                writeln(writer, "# " + (new Date()).toString())
                writeln(writer, "")
            }
            else {
                lines.add(pos + 1, new CommentLine("# " + (new Date()).toString()))
            }
        }

        // Process each line.
        lines.each {Line line ->
            writeln(writer, line.toString())
        }

        writer.close()
    }

    /**
     * This method does the conversion of strings during storing of the
     * properties. This means replacing special characters with unicode
     * literals.
     *
     * @param aString The string to be converted.
     *
     * @return The converted string.
     */
    @TypeChecked(TypeCheckingMode.SKIP)
    private static String storeConvert(String aString) {
        // Initialization.
        int length = aString.length()
        StringBuffer result = new StringBuffer(length * 2)

        // For each character...
        for(int j = 0; j < length;) {
            char c = aString.charAt(j++)

            // Depending on the character type:
            switch(c) {
                case '\\':
                    result.append('\\')
                    break
                case '\t':
                    result.append('\t')
                    break
                case '\n':
                    result.append('\n')
                    break
                case '\r':
                    result.append('\r')
                    break
                case '\f':
                    result.append('\f')
                    break
                default:
                    if (c < ('\024' as char) || c > ('\177' as char)) {
                        result.append('\\')
                        result.append('u')
                        result.append(toHex(c >> 12 & 0xf))
                        result.append(toHex(c >> 8 & 0xf))
                        result.append(toHex(c >> 4 & 0xf))
                        result.append(toHex(c & 0xf))
                        break
                    }
                    if ("\t\r\n\f\"".indexOf(c.toString()) != -1) {
                        result.append('\\')
                    }
                    result.append(c)
                    break
            }
        }
        return result.toString()
    }

    /**
     * Removes the mapping specified by the given key from the property set. <p>
     * Note: Comment lines belonging to a property will not be deleted.
     *
     * @param aKey The key of the property to remove.
     *
     * @return The value for the key.
     */
    @Override
    synchronized Object remove(Object aKey) {
        Object value = super.remove(aKey)
        if (value != null) {
            // Try to find a line that contains the key.
            for(int i = 0; i < lines.size(); i++) {
                // Get Line.
                Line line = lines.get(i)

                // Is it a property line?
                if (line instanceof PropertyLine) {
                    PropertyLine propLine = (PropertyLine)line
                    if (propLine.key.equals(aKey)) {
                        // Key found -> remove the corresponding line
                        lines.set(i, new CommentPropertyLine("#", propLine.key, propLine.value))
                    }
                }
            }
        }
        return value
    }

    synchronized Object remove(Object aKey, String[] comment) {
        if (super.containsKey(aKey)) {
            addComment(aKey.toString(), comment)
        }

        return remove(aKey)
    }

    /**
     * This helper method converts a given integer to a hex digit.
     *
     * @param anInt The integer to convert.
     *
     * @return The hex digit for the given integer.
     */
    private static char toHex(int anInt) {
        return hexDigit[anInt & 0xf]
    }

    /**
     * This method writes the given string to the given writer, followed by a
     * newline.
     *
     * @param aWriter The writer to write to.
     *
     * @param aString The string to be written.
     */
    private static void writeln(BufferedWriter aWriter, String aString) throws IOException {
        aWriter.write(aString)
        aWriter.newLine()
    }

    /**
     * Mutating views is not supported with {@link SimpleVersionProperties}.
     */
    @Override
    Collection<Object> values() {
        return Collections.unmodifiableCollection(super.values())
    }

    /**
     * Mutating views is not supported with {@link SimpleVersionProperties}.
     */
    @Override
    Set<Object> keySet() {
        return Collections.unmodifiableSet(super.keySet())
    }

    /**
     * Mutating views is not supported with {@link SimpleVersionProperties}.
     */
    @Override
    Set<Map.Entry<Object, Object>> entrySet() {
        return Collections.unmodifiableSet(super.entrySet())
    }

    /**
     * This inner class represents a line in a properties file.
     */
    abstract class Line { }

    /**
     * This inner class represents an empty line in a properties file.
     */
    class EmptyLine extends Line {
        @Override
        boolean equals(Object obj) {
            return true
        }

        @Override
        String toString() {
            return ''
        }
    }

    /**
     * This inner class represents a comment line in a properties file.
     */

    class CommentLine extends Line {
        String comment

        CommentLine(String aComment){
            comment = aComment
        }

        @Override
        boolean equals(Object obj) {
            if (obj instanceof CommentLine) {
                CommentLine nPL = (CommentLine)obj
                return (comment.equals(nPL.comment))
            }
            return false
        }

        @Override
        String toString() {
            return "${comment}".toString()
        }
    }

    /**
     * This inner class represents a property line in a properties file.
     */
    class PropertyLine extends Line {
        String key
        String value

        PropertyLine(String aKey, String aValue) {
            key = aKey
            value = aValue
        }

        @Override
        boolean equals(Object obj)
        {
            if (obj instanceof PropertyLine)
            {
                PropertyLine nPL = (PropertyLine)obj
                return (key.equals(nPL.key) && value.equals(nPL.value))
            }
            return false
        }

        @Override
        String toString() {
            return "${storeConvert(key)} = ${storeConvert(value)}".toString()
        }
    }

    /**
     * This inner class represents an comment line with a property.
     */

    class CommentPropertyLine extends PropertyLine {
        String comment

        CommentPropertyLine(String aComment, String aKey, String aValue)
        {
            super(aKey, aValue)
            comment = aComment
        }

        @Override
        boolean equals(Object obj) {
            if (obj instanceof CommentPropertyLine) {
                CommentPropertyLine nPL = (CommentPropertyLine)obj
                return (comment.equals(nPL.comment) && key.equals(nPL.key) && value.equals(nPL.value))
            }
            return false
        }

        @Override
        String toString() {
            if(comment) {
                return "# ${storeConvert(key)} = ${storeConvert(value)}".toString()
            } else {
                super.toString()
            }
        }
    }

}
