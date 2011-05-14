/*
 * Copyright 2010 Alibaba Group Holding Limited.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.alibaba.citrus.util.templatelite;

import static com.alibaba.citrus.test.TestUtil.*;
import static com.alibaba.citrus.util.ArrayUtil.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import com.alibaba.citrus.util.templatelite.Template.IncludeTemplate;
import com.alibaba.citrus.util.templatelite.Template.Node;
import com.alibaba.citrus.util.templatelite.Template.Placeholder;
import com.alibaba.citrus.util.templatelite.Template.PlaceholderParameter;
import com.alibaba.citrus.util.templatelite.Template.Text;

public class TemplateParserTests extends AbstractTemplateTests {
    @Test
    public void test01_text_simple() throws Exception {
        loadTemplate("test01_text_simple.txt", 1, 0, 0);

        assertText("hello,\n  world", template.nodes[0]);
    }

    @Test
    public void test01_text_failure() throws Exception {
        String s;

        // text��#abc����
        s = "";
        s += "#abc\n";
        s += "#end\n";
        s += "\n";
        s += "  hello";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid text here at test.txt: Line 4 Column 3"));

        // \#text��#abc����
        s = "";
        s += "#abc\n";
        s += "#end\n";
        s += "\n";
        s += "  \\#hello";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid text here at test.txt: Line 4 Column 3"));
    }

    @Test
    public void test02_placeholder_simple() throws Exception {
        loadTemplate("test02_placeholder_simple.txt", 8, 0, 0);

        int i = 0;
        assertText("a${123}", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "a123", "Line 1 Column 8");
        assertText("${abc}", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "abc", "Line 1 Column 22");
        assertPlaceholder(template.nodes[i++], "a", "Line 1 Column 30", new String[] { "123" }, "123");
        assertPlaceholder(template.nodes[i++], "a", "Line 1 Column 38");
        assertPlaceholder(template.nodes[i++], "a", "Line 1 Column 45", new String[] { "1", "2", "3" }, "1,2,3");
        assertText("b", template.nodes[i++]);
    }

    @Test
    public void test02_placeholder_template() throws Exception {
        loadTemplate("test02_placeholder_template.txt", 2, 2, 0);

        Placeholder placeholder = (Placeholder) template.nodes[0];

        assertPlaceholder(placeholder, "for", "Line 1 Column 1", new String[] { "#aaa", "#bbb.ccc", "ccc" },
                "#aaa, #bbb.ccc, ccc");

        PlaceholderParameter param;
        int i = 0;

        param = placeholder.params[i++];
        assertTrue(param.isTemplateReference());
        assertEquals("aaa", param.getTemplateName());
        assertEquals("aaa", param.getTemplateReference().getName());

        param = placeholder.params[i++];
        assertTrue(param.isTemplateReference());
        assertEquals("bbb.ccc", param.getTemplateName());
        assertEquals("ccc", param.getTemplateReference().getName());

        param = placeholder.params[i++];
        assertFalse(param.isTemplateReference());
        assertNull(param.getTemplateName());
        assertNull(param.getTemplateReference());
    }

    @Test
    public void test02_placeholder_template_group() throws Exception {
        loadTemplate("test02_placeholder_template_group.txt", 1, 1, 1);

        Placeholder placeholder = (Placeholder) template.nodes[0];

        assertPlaceholder(placeholder, "for", "Line 3 Column 1", new String[] { "#aaa.b", "#aaa.c", "#aaa.d", "ccc" },
                "#aaa.*, ccc");

        PlaceholderParameter param;
        int i = 0;

        param = placeholder.params[i++];
        assertTrue(param.isTemplateReference());
        assertEquals("aaa.b", param.getTemplateName());
        assertEquals("b", param.getTemplateReference().getName());

        param = placeholder.params[i++];
        assertTrue(param.isTemplateReference());
        assertEquals("aaa.c", param.getTemplateName());
        assertEquals("c", param.getTemplateReference().getName());

        param = placeholder.params[i++];
        assertTrue(param.isTemplateReference());
        assertEquals("aaa.d", param.getTemplateName());
        assertEquals("d", param.getTemplateReference().getName());

        param = placeholder.params[i++];
        assertFalse(param.isTemplateReference());
        assertNull(param.getTemplateName());
        assertNull(param.getTemplateReference());
    }

    @Test
    public void test02_placeholder_failure() throws Exception {
        String s;

        // ${}��#abc����
        s = "";
        s += "#abc\n";
        s += "#end\n";
        s += "\n";
        s += "  ${hello}";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid ${hello} here at test.txt: Line 4 Column 3"));

        // ${}��#abc����
        s = "";
        s += "#abc\n";
        s += "#end\n";
        s += "\n";
        s += "  ${hello:param1, param2}";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid ${hello:param1, param2} here at test.txt: Line 4 Column 3"));

        // keyword
        s = "";
        s += "This\n";
        s += "is a keyword: \n";
        s += "${placeholder}\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Reserved name: placeholder at test.txt: Line 3 Column 1"));

        // ${xxx: #notexist}ģ��δ�ҵ�
        s = "";
        s += "  ${xxx: aaa, #notexist}";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Referenced template notexist is not found in the context around test.txt: Line 1 Column 3"));

        // ${xxx: #a.b.c}ģ��δ�ҵ�
        s = "";
        s += "  ${xxx: #a.b.c}";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Referenced template a.b.c is not found in the context around test.txt: Line 1 Column 3"));

        s = "";
        s += "  ${xxx: #a.b.c}\n";
        s += "#a\n";
        s += "#b\n";
        s += "#end\n";
        s += "#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Referenced template a.b.c is not found in the context around test.txt: Line 1 Column 3"));

        // ${xxx: #a.b.*}ģ��δ�ҵ�
        s = "";
        s += "  ${xxx: #a.b.*}";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Referenced template a.b is not found in the context around test.txt: Line 1 Column 3"));

        s = "";
        s += "  ${xxx: #a.b.*}\n";
        s += "#a\n";
        s += "#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Referenced template a.b is not found in the context around test.txt: Line 1 Column 3"));
    }

    @Test
    public void test03_subtemplate_simple() throws Exception {
        loadTemplate("test03_subtemplate_simple.txt", 1, 2, 0);

        int i = 0;
        assertText("a\\#abc#123$#{abc}#@xxx", template.nodes[i++]);

        assertTemplate(template.getSubTemplate("abc"), "abc", 0, 1, 0, "Line 2 Column 3");
        assertTemplate(template.getSubTemplate("abc").getSubTemplate("def"), "def", 1, 0, 0, "Line 3 Column 5");
        assertText("hello", template.getSubTemplate("abc").getSubTemplate("def").nodes[0]);
        assertTemplate(template.getSubTemplate("def"), "def", 0, 0, 0, "Line 9 Column 1");
    }

    @Test
    public void test03_subtemplate_failure() throws Exception {
        String s;

        // #abcǰ������
        s = "";
        s += "hhhhh  #abc\n";
        s += "#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("#abc should start at new line, which is now at test.txt: Line 1 Column 8"));

        // #abc���#end
        s = "";
        s += "hhhhh\n";
        s += "  #abc#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid content followed after #abc at test.txt: Line 2 Column 7"));

        // #abc���content
        s = "";
        s += "hhhhh\n";
        s += "  #abc content\n";
        s += "#end";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid content followed after #abc at test.txt: Line 2 Column 8"));

        // ȱ��#end
        s = "";
        s += "#abc\n";
        s += "#def\n";
        s += "#ghi\n";
        s += "#end";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Unclosed tags: #def, #abc at test.txt: Line 5"));

        // systemIdδָ��ʱ�Ĵ�����Ϣ
        loadTemplateFailure(s.getBytes(), null);
        assertThat(parseError, exception("Unclosed tags: #def, #abc at [unknown source]: Line 5"));

        // #end̫��
        s = "";
        s += "#abc\n";
        s += "#def\n";
        s += "#ghi\n";
        s += "#end\n";
        s += "#end\n";
        s += "#end\n";
        s += "#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Unmatched #end tag at test.txt: Line 7 Column 1"));

        // template name�ظ�
        s = "";
        s += " #abc\n";
        s += "#end\n";
        s += "  #abc\n";
        s += "#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Duplicated template name #abc at test.txt: Line 3 Column 3.  "
                + "Another template with the same name is located in test.txt: Line 1 Column 2"));

        // keyword
        s = "";
        s += "This\n";
        s += "is a keyword: \n";
        s += "#text\n";
        s += "#end";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Reserved name: text at test.txt: Line 3 Column 1"));
    }

    @Test
    public void test04_include_template_simple() {
        loadTemplate("test04_include_template_simple.txt", 5, 2, 0);

        int i = 0;
        assertText("a$#{123}", template.nodes[i++]);
        assertIncludeTemplate(template.nodes[i++], "a123", "Line 1 Column 9", template.getSubTemplate("a123"));
        assertText("$#{abc}", template.nodes[i++]);
        assertIncludeTemplate(template.nodes[i++], "abc", "Line 1 Column 25", template.getSubTemplate("abc"));
        assertText("$#{a:123}$#{a : }$#{a:1,2,3}b", template.nodes[i++]);

        assertTemplate(template.getSubTemplate("a123"), "a123", 0, 0, 0, "Line 2 Column 1");
        assertTemplate(template.getSubTemplate("abc"), "abc", 0, 0, 0, "Line 4 Column 1");
    }

    @Test
    public void test04_include_template_override() {
        loadTemplate("test04_include_template_override.txt", 0, 3, 0);

        Template b_level0 = template.getSubTemplate("bbb");
        Template a_level2 = template.getSubTemplate("level1").getSubTemplate("level2").getSubTemplate("aaa");

        Template level2 = template.getSubTemplate("level1").getSubTemplate("level2");

        IncludeTemplate includeA = (IncludeTemplate) level2.nodes[1];
        IncludeTemplate includeB = (IncludeTemplate) level2.nodes[2];

        assertSame(a_level2, includeA.includedTemplate);
        assertSame(b_level0, includeB.includedTemplate);
    }

    @Test
    public void test04_include_template_sub() {
        loadTemplate("test04_include_template_sub.txt", 1, 1, 1);

        IncludeTemplate include = (IncludeTemplate) template.nodes[0];

        assertEquals("level1.level2.aaa", include.templateName);
        assertSame(template.getSubTemplate("level1").getSubTemplate("level2").getSubTemplate("aaa"),
                include.includedTemplate);
    }

    @Test
    public void test04_include_template_failure() throws Exception {
        String s;

        // $#{xxx}��#abc����
        s = "";
        s += "#abc\n";
        s += "#end\n";
        s += "\n";
        s += "  $#{abc}";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid $#{abc} here at test.txt: Line 4 Column 3"));

        // $#{notexist}ģ��δ�ҵ�
        s = "";
        s += "  $#{notexist}";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Included template notexist is not found in the context around test.txt: Line 1 Column 3"));

        // $#{abc}ģ��δ�ҵ���Ƕ�ף�
        s = "";
        s += "#xxx\n";
        s += "  #yyy\n";
        s += "    hello, $#{abc}\n";
        s += "  #end\n";
        s += "#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Included template abc is not found in the context around test.txt: Line 3 Column 12"));

        // $#{a.b.c}ģ��δ�ҵ�����ģ�壩
        s = "$#{a.b.c}\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Included template a.b.c is not found in the context around test.txt: Line 1 Column 1"));

        s = "$#{a.b.c}\n";
        s += "#a\n";
        s += "#b\n";
        s += "#end\n";
        s += "#end\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError,
                exception("Included template a.b.c is not found in the context around test.txt: Line 1 Column 1"));
    }

    @Test
    public void test05_param_charset() {
        loadTemplate("test05_param_utf8.txt", 1, 0, 1);

        assertText("\n��ã�", template.nodes[0]);
        assertEquals("UTF-8", template.getParameter("charset"));

        loadTemplate("test05_param_gbk.txt", 1, 0, 1);

        assertText("\n��ã�", template.nodes[0]);
        assertEquals("GBK", template.getParameter("charset"));
    }

    @Test
    public void test05_param_override() {
        loadTemplate("test05_param_override.txt", 0, 1, 2);

        assertEquals("111", template.getParameter("xxx"));
        assertEquals(null, template.getParameter("yyy"));
        assertEquals("", template.getParameter("zzz"));

        assertEquals("222", template.getSubTemplate("level1").getParameter("xxx"));
        assertEquals("444", template.getSubTemplate("level1").getParameter("yyy"));
        assertEquals("", template.getSubTemplate("level1").getParameter("zzz"));

        assertEquals("333", template.getSubTemplate("level1").getSubTemplate("level2").getParameter("xxx"));
        assertEquals("444", template.getSubTemplate("level1").getSubTemplate("level2").getParameter("yyy"));
        assertEquals("", template.getSubTemplate("level1").getSubTemplate("level2").getParameter("zzz"));
    }

    @Test
    public void test05_param_failure() throws Exception {
        String s;

        // #@paramǰ������
        s = "";
        s += "hhhhh  #@param\n";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("#@param should start at new line, which is now at test.txt: Line 1 Column 8"));

        // #@param��text�ĺ���
        s = "";
        s += "\n";
        s += "abc\n";
        s += "  #@hello";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid #@hello here at test.txt: Line 3 Column 3"));

        // #@param��${abc}�ĺ���
        s = "";
        s += "\n";
        s += "  ${abc}\n";
        s += "  #@hello";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid #@hello here at test.txt: Line 3 Column 3"));

        // #@param��#abc����
        s = "";
        s += "#abc\n";
        s += "#end\n";
        s += "\n";
        s += "  #@hello";

        loadTemplateFailure(s.getBytes(), "test.txt");
        assertThat(parseError, exception("Invalid #@hello here at test.txt: Line 4 Column 3"));
    }

    @Test
    public void test05_param_trim() throws Exception {
        String s;

        // no trimming
        s = "";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   hello, ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";

        loadTemplate(s.getBytes(), "test.txt", 3, 0, 2);

        int i = 0;
        assertText("  \n\n   hello, ", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "name", "Line 5 Column 11");
        assertText("   \n     hi    \n     \n     \nha\n     ", template.nodes[i++]);

        // with trimming
        s = "";
        s += "#@trimming on\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   hello, ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     ";

        loadTemplate(s.getBytes(), "test.txt", 3, 0, 3);

        i = 0;
        assertText("hello, ", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "name", "Line 6 Column 11");
        assertText("\nhi\n\n\nha", template.nodes[i++]);

        // with trimming, drop first/last
        s = "";
        s += "#@trimming on\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";
        s += "${xxx}    \n";

        loadTemplate(s.getBytes(), "test.txt", 3, 0, 3);

        i = 0;
        assertPlaceholder(template.nodes[i++], "name", "Line 6 Column 4");
        assertText("\nhi\n\n\nha\n\n", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "xxx", "Line 12 Column 1");
    }

    @Test
    public void test05_param_trim_subtemplate() throws Exception {
        String s;

        // no trimming
        s = "";
        s += "#sub\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   hello, ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";
        s += "#end";

        loadTemplate(s.getBytes(), "test.txt", 0, 1, 0);
        template = template.getSubTemplate("sub");

        int i = 0;
        assertText("  \n\n   hello, ", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "name", "Line 6 Column 11");
        assertText("   \n     hi    \n     \n     \nha\n     ", template.nodes[i++]);

        // with trimming
        s = "";
        s = "#@trimming YES\n";
        s += "#sub\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   hello, ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";
        s += "#end";

        loadTemplate(s.getBytes(), "test.txt", 0, 1, 1);
        template = template.getSubTemplate("sub");

        i = 0;
        assertText("hello, ", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "name", "Line 7 Column 11");
        assertText("\nhi\n\n\nha", template.nodes[i++]);

        // with trimming, drop first/last
        s = "";
        s += "#@trimming on\n";
        s += "#sub\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";
        s += "${xxx}    \n";
        s += "#end";

        loadTemplate(s.getBytes(), "test.txt", 0, 1, 1);
        template = template.getSubTemplate("sub");

        i = 0;
        assertPlaceholder(template.nodes[i++], "name", "Line 7 Column 4");
        assertText("\nhi\n\n\nha\n\n", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "xxx", "Line 13 Column 1");
    }

    @Test
    public void test05_param_collapse_ws() throws Exception {
        String s;

        // with collapsing
        s = "";
        s += "#@whitespace collapse\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   hello,   ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";

        loadTemplate(s.getBytes(), "test.txt", 3, 0, 3);

        int i = 0;
        assertText("\nhello, ", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "name", "Line 6 Column 13");
        assertText("\nhi\nha\n", template.nodes[i++]);

        // with trimming & collapsing, drop first/last
        s = "";
        s += "#@trimming on\n";
        s += "#@whitespace COLLAPSE\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";
        s += "${xxx}    \n";

        loadTemplate(s.getBytes(), "test.txt", 3, 0, 4);

        i = 0;
        assertPlaceholder(template.nodes[i++], "name", "Line 7 Column 4");
        assertText("\nhi\nha\n", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "xxx", "Line 13 Column 1");
    }

    @Test
    public void test05_param_collapse_ws_subtemplate() throws Exception {
        String s;

        // with collapsing
        s = "";
        s += "#sub\n";
        s += "#@whitespace collapse\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   hello,   ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";
        s += "#end";

        loadTemplate(s.getBytes(), "test.txt", 0, 1, 0);
        template = template.getSubTemplate("sub");

        int i = 0;
        assertText("\nhello, ", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "name", "Line 7 Column 13");
        assertText("\nhi\nha\n", template.nodes[i++]);

        // with trimming & collapsing, drop first/last
        s = "";
        s += "#sub\n";
        s += "#@trimming on\n";
        s += "#@whitespace COLLAPSE\n";
        s += "#@param1\n";
        s += "  \n";
        s += "#@param2\n";
        s += "\n";
        s += "   ${name}   \n";
        s += "     hi    \n";
        s += "     \n";
        s += "     \n";
        s += "ha\n";
        s += "     \n";
        s += "${xxx}    \n";
        s += "#end\n";

        loadTemplate(s.getBytes(), "test.txt", 0, 1, 0);
        template = template.getSubTemplate("sub");

        i = 0;
        assertPlaceholder(template.nodes[i++], "name", "Line 8 Column 4");
        assertText("\nhi\nha\n", template.nodes[i++]);
        assertPlaceholder(template.nodes[i++], "xxx", "Line 14 Column 1");
    }

    @Test
    public void test06_real_case() throws Exception {
        loadTemplate("test06_real_case.txt", 5, 1, 3);

        int i = 0;
        assertText("<html>\n" //
                + "<head>\n" //
                + "<title>", template.nodes[i++]);

        assertPlaceholder(template.nodes[i++], "title", "Line 7 Column 12", new String[] { "�ҵı���" }, "�ҵı���");

        assertText("</title>\n" //
                + "</head>\n" //
                + "<body>\n", template.nodes[i++]);

        assertIncludeTemplate(template.nodes[i++], "itemlist", "Line 10 Column 5", template.getSubTemplate("itemlist"));

        assertText("\n</body>\n" //
                + "</html>", template.nodes[i++]);

        // -------------------------------
        Template itemlist = template.getSubTemplate("itemlist");

        i = 0;
        assertText("<ul>\n", itemlist.nodes[i++]);
        assertPlaceholder(itemlist.nodes[i++], "item", "Line 18 Column 5",
                new String[] { "#dateItem", "#datetimeItem" }, "#dateItem, #datetimeItem");
        assertText("\n</ul>", itemlist.nodes[i++]);

        // -------------------------------
        Template dateItem = itemlist.getSubTemplate("dateItem");

        i = 0;
        assertText("<li>  ", dateItem.nodes[i++]); // spaces are not collpased
        assertPlaceholder(dateItem.nodes[i++], "date", "Line 22 Column 11", new String[] { "yyyy-MM-dd" }, "yyyy-MM-dd");
        assertText("  </li>", dateItem.nodes[i++]);

        // -------------------------------
        Template datetimeItem = itemlist.getSubTemplate("datetimeItem");

        i = 0;
        assertText("<li> ", datetimeItem.nodes[i++]);
        assertPlaceholder(datetimeItem.nodes[i++], "date", "Line 26 Column 10", new String[] { "yyyy-MM-dd", "HH:mm" },
                "yyyy-MM-dd,HH:mm");
        assertText(" </li>", datetimeItem.nodes[i++]);
    }

    @Test
    public void test07_predefined_templates() {
        loadTemplate("test07_predefined_templates.txt", 6, 2, 1);

        String expected = "";

        expected += "<a href=\"test\" onclick=\"alert('hi')\">hello\n";
        expected += "</a>";

        assertEquals(expected, template.renderToString(new FallbackTextWriter<StringBuilder>()));
    }

    private void assertText(String text, Node node) {
        Text t = (Text) node;

        assertEquals(text, t.text);

        String str = t.toString();

        assertThat(str, containsAll("Text with ", "characters: "));
    }

    private void assertPlaceholder(Node node, String name, String location) {
        assertPlaceholder(node, name, location, new String[0], null);
    }

    private void assertPlaceholder(Node node, String name, String location, String[] params, String paramsString) {
        Placeholder placeholder = (Placeholder) node;

        assertEquals(name, placeholder.name);
        assertEquals(paramsString, placeholder.paramsString);
        assertLocation(placeholder.location, location);

        if (isEmptyArray(params)) {
            assertEquals(0, placeholder.params.length);
        } else {
            assertEquals(params.length, placeholder.params.length);

            for (int i = 0; i < params.length; i++) {
                assertEquals(params[i], placeholder.params[i].getValue());
            }
        }

        String str = placeholder.toString();

        if (paramsString == null) {
            assertThat(str, startsWith("${" + name + "}"));
        } else {
            assertThat(str, startsWith("${" + name + ":" + paramsString + "}"));
        }

        assertLocation(str, location);
    }

    private void assertIncludeTemplate(Node node, String templateName, String location, Template template) {
        IncludeTemplate includeTemplate = (IncludeTemplate) node;

        assertEquals(templateName, includeTemplate.templateName);
        assertLocation(includeTemplate.location, location);

        String str = includeTemplate.toString();

        assertThat(str, startsWith("$#{" + templateName + "}"));
        assertLocation(str, location);

        assertNotNull(includeTemplate.includedTemplate);
        assertSame(template, includeTemplate.includedTemplate);
    }

}