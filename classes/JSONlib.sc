JSONlibNull {}


JSONlib {
	*dumps {|dict, customEncoder=nil, postWarnings=true|
		if(dict.isKindOf(Dictionary).not, {
			"Can only convert a Dictonary/Event to JSON but received %".format(dict.class).warn;
			^"{ }";
		});
		customEncoder = customEncoder ? {};
		^JSONlib.prConvertToJson(dict, postWarnings, customEncoder);
	}

	*parse {|string, toEvent=true, postWarnings=true|
		if(string.isKindOf(String).not, {
			"Can only parse a String to JSON but received %".format(string.class).warn;
			^();
		});
		^JSONlib.prConvertToSC(string.parseJSON, toEvent, postWarnings);
	}

	*parseFile {|filePath, toEvent=true, postWarnings=true|
		^JSONlib.prConvertToSC(filePath.parseJSONFile, toEvent, postWarnings);
	}

	*prConvertToJson {|v, postWarnings=true, customEncoder|
		var array;
		^case
		{ v.isKindOf(Symbol) } { JSONlib.prConvertToJson(v.asString, postWarnings, customEncoder) }
		{ (v == "null").or(v.class == JSONlibNull) } { "null" }
		// sc closely implements the JSON string, see https://www.json.org/json-en.html
		// but the post window parses \n as linebreak etc. which makes copying of the JSON from
		// the post window error prone
		{ v.isString } { v.replace($", "\\\"").quote }
		{ v.isNumber } { v.asCompileString }
		{ v.isKindOf(Boolean) } { v.asBoolean }
		{ v.isKindOf(SequenceableCollection) } {
			array = v.collect { |x| JSONlib.prConvertToJson(x, postWarnings, customEncoder) };
			if(postWarnings and: { v.class !== Array  }) {
				"JSON file format will not recover % class, but instead an Array".format(v.class.name).warn
			};
			"[ % ]".format(array.join(", "))
		}
		{ v.isKindOf(Dictionary) } {
			array = v.asAssociations.collect { |x|
				var key = x.key;
				if((key.isKindOf(String)).not, {
					"Key % got transformed to a string".format(key).warn;
					key = key.asString.quote
				});
				"%: %".format(key, JSONlib.prConvertToJson(x.value, postWarnings, customEncoder))
			};
			/*
			this can be documented as I rarely come across people who use dictionaries and
			is also optional depending on the parsing flags

			if(postWarnings and: { v.class !== Dictionary }) {
				"JSON file format will not recover % class, but instead a Dictionary".format(v.class.name).warn
			};
			*/
			"{ % }".format(array.join(", "))
		}
		{
			if(postWarnings) { "JSON file format will not recover % class, but instead a compile string".format(v.class.name).warn };
			v.asCompileString.quote
		}
	}

	*prConvertToSC { |v, toEvent, postWarnings|
		var res;
		^case
		{ v.isString and: { v.every { |x| x.isDecDigit } } } { v.asInteger }
		// see https://www.json.org/json-en.html Number section and
		// https://stackoverflow.com/a/6425559/3475778
		{ v.isString and: "^[-]?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?$".matchRegexp(v.asString) } { v.asFloat }
		{ v == "true" } { true }
		{ v == "false" } { false }
		// an event can not store nil as a value so we replace it with JSONNull
		{ v == nil } { if(toEvent, {JSONlibNull()}, {nil}) }
		{ v.isArray } { v.collect { |x| JSONlib.prConvertToSC(x, toEvent, postWarnings) } }
		{ v.isKindOf(Dictionary) } {
			if(toEvent) {
				res = Event.new;
				v.pairsDo { |key, x|
					res.put(key.asSymbol, JSONlib.prConvertToSC(x, toEvent, postWarnings));
				};
				res;
			} {
				v.collect { |x| JSONlib.prConvertToSC(x, toEvent, postWarnings) }
			}
		}
		{ v }
	}
}


TestJSONlib : UnitTest {
	*prJsonFilePath {|fileName|
		^this.class.filenameSymbol.asString.dirname +/+ "assets" +/+ fileName;
	}

	*prLoadJsonFile {|fileName|
		^JSONlib.parseFile(TestJSONlib.prJsonFilePath(fileName));
	}

	// decoding tests - taken from json.org
	// we only test for valid json
	test_object {
		var j = TestJSONlib.prLoadJsonFile("object.json");
		this.assertEquals(j[\foo][\bar], "baz", "Parse nested objects");
	}

	test_array {
		var j = TestJSONlib.prLoadJsonFile("array.json");
		this.assertEquals(j, [20 , 30, 40], "JSON can contain also array as root");
	}

	test_values {
		var j = TestJSONlib.prLoadJsonFile("values.json");
		this.assertEquals(j[\string], "string", "Value can be strings");
		this.assertEquals(j[\number], 10, "Value can be integer numebrs");
		this.assertEquals(j[\object][\foo], "bar", "Values can be objects");
		this.assertEquals(j[\array], [1, 2, 3], "Value can be an array");
		this.assertEquals(j[\true], true, "Value can be true");
		this.assertEquals(j[\false], false, "Value can be false");
		this.assertEquals(j[\null].class, JSONlibNull, "Value can be null");
	}

	test_strings {
		var j = TestJSONlib.prLoadJsonFile("strings.json");
		this.assertEquals(j[\text], "lorem ipsum", "Standard text should be reproduced");
		this.assertEquals(j[\quotationMark], "lorem\"ipsum", "Quotation needs to be escaped");
		this.assertEquals(j[\reverseSolidus], "lorem\\ipsum", "Reverse Solidus needs to be escaped");
		this.assertEquals(j[\solidus], "lorem/ipsum", "Solidus does not need a SC escape");
		this.assertEquals(j[\backspace], "lorem%ipsum".format(0x08.asAscii), "Backspace needs escape");
		this.assertEquals(j[\formfeed], "lorem%ipsum".format(0x0c.asAscii), "Formfeed needs escpae");
		this.assertEquals(j[\linefeed], "lorem\nipsum", "Linebreak needs an escape");
		this.assertEquals(j[\carriageReturn], "lorem\ripsum", "carriage return needs an escape");
		this.assertEquals(j[\horizontalTab], "lorem\tipsum", "tab needs an escape");

		// sc can not compare utf-8 chares so we make some extra steps
		this.assertEquals(
			j[\hexDigits].ascii[0..4].asAscii,
			"lorem",
			"Hex encoding should not affect ascii chars lorem",
		);

		this.assertEquals(
			j[\hexDigits].ascii[5..8].wrap(0, 255),
			// normaly hao is represented as e5a5bd in utf-8
			[0xe5, 0xa5, 0xbd, 0x69],
			"Hex encoding should be parsed for 好",
		);

		this.assertEquals(
			j[\hexDigits].ascii[8..].asAscii,
			"ipsum",
			"Hex encoding should not affect ascii chars ipsum",
		);
	}

	test_numbers {
		var j = TestJSONlib.prLoadJsonFile("numbers.json");
		this.assertEquals(j[\integer], 10, "Positive integer");
		this.assertEquals(j[\negativeInteger], -1 * 10, "Negative integer");
		this.assertEquals(j[\float], 10.0, "float");
		this.assertEquals(j[\negativeFloat], -10.0, "negative float");
		this.assertEquals(j[\bigE], 20000000000.0, "Big E writing");
		this.assertEquals(j[\smallE], 20000000000.0, "small e writing");
		this.assertEquals(j[\negativeExponent], 0.0000000002, "negative exponent");
	}

	test_jsonNull {
		var p = TestJSONlib.prJsonFilePath("values.json");
		var j = JSONlib.parseFile(p, toEvent: true);
		this.assertEquals(j[\null].class, JSONlibNull, "As an Event can not store nil as value we implemented JSONlibNull");
		j = JSONlib.parseFile(p, toEvent: false);
		this.assertEquals(j["null"], nil, "the SC dict can store nil");
	}
}
