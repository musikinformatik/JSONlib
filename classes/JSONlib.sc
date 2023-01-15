JSONlibNull {}


JSONlib {

	var <>postWarnings, <>useEvent, <>customEncoder;

	*new { |postWarnings = true, useEvent=true, customEncoder|
		^super.newCopyArgs(postWarnings, useEvent, customEncoder)
	}

	*convertToJSON {|object, customEncoder=nil, postWarnings=true|
		if((object.isKindOf(Dictionary) or: (object.isKindOf(SequenceableCollection))).not) {
			Error("Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class)).throw
		};
		^this.new(postWarnings, customEncoder: customEncoder).prConvertToJson(object);
	}

	*convertToSC {|string, useEvent=true, postWarnings=true|
		if(string.isKindOf(String).not) {
			Error("Can only parse a String to JSON but received %".format(string.class)).throw
		};
		^this.new(postWarnings, useEvent: useEvent).prConvertToSC(string.parseJSON)
	}

	*parseFile {|filePath, useEvent=true, postWarnings=true|
		^this.new(postWarnings, useEvent: useEvent).prConvertToSC(filePath.parseJSONFile);
	}

	prConvertToJson {|v, postWarnings=true, customEncoder|
		var array;
		^case
		{ v.isKindOf(Symbol) } { this.prConvertToJson(v.asString) }
		{ v == "null" or: { v.class == JSONlibNull } or: { v == nil } } { "null" }
		// sc closely implements the JSON string, see https://www.json.org/json-en.html
		// but the post window parses \n as linebreak etc. which makes copying of the JSON from
		// the post window error prone
		{ v.isString } { v
			.replace("\\", "\\\\") // reverse solidus
			.replace("/", "\\/") // solidus
			.replace($", "\\\"") // quoatition mark
			.replace(0x08.asAscii, "\\b") // backspace
			.replace(0x0c.asAscii, "\\f") // formfeed
			.replace("\n", "\\n") // linefeed
			.replace("\r", "\\r") // carriage return
			.replace("\t", "\\t") // horizontal tab
			// @todo non ascii chars
			.quote
		}
		{ v.isNumber } {
			case
			{v==inf} { "inf".quote }
			{v==inf.neg} { "-inf".quote }
			{v.asCompileString};
		}
		{ v.isKindOf(Boolean) } { v.asBoolean }
		{ v.isKindOf(SequenceableCollection) } {
			array = v.collect { |x| this.prConvertToJson(x) };
			if(postWarnings and: { v.class !== Array  }) {
				"JSON file format will not recover % class, but instead an Array".format(v.class.name).warn
			};
			"[ % ]".format(array.join(", "))
		}
		{ v.isKindOf(Dictionary) } {
			array = v.asAssociations.sort.collect { |x|
				var key = x.key;
				if((key.isKindOf(String)).not) {
					if(key.isKindOf(Symbol).not) {
						"Key % of type % got transformed to a string".format(key, key.class).warn;
				};
					key = key.asString;
			};
				"%: %".format(key.quote, this.prConvertToJson(x.value))
			};
			"{ % }".format(array.join(", "))
		}
		{
			if(postWarnings) { "JSON file format will not recover % class, but instead a compile string".format(v.class.name).warn };
			this.prConvertToJson(v.asCompileString);
		}
	}

	prConvertToSC { |v|
		var res;
		^case
		{ v.isString and: { v.every { |x| x.isDecDigit } } } { v.asInteger }
		// see https://www.json.org/json-en.html Number section and
		// https://stackoverflow.com/a/6425559/3475778
		{ v.isString and: { "^[-]?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?$".matchRegexp(v.asString) } } { v.asFloat }
		{ v == "true" } { true }
		{ v == "false" } { false }
		// an event can not store nil as a value so we replace it with JSONNull
		{ v == nil } { if(useEvent, {JSONlibNull()}, {nil}) }
		{ v.isArray } { v.collect { |x| this.prConvertToSC(x) } }
		{ v.isKindOf(Dictionary) } {
			if(useEvent) {
				res = Event.new;
				v.pairsDo { |key, x|
					res.put(key.asSymbol, this.prConvertToSC(x));
				};
				res;
			} {
				v.collect { |x| this.prConvertToSC(x) }
			}
		}
		{ v }
	}
}


TestJSONlib : UnitTest {
	// util
	*prJsonFilePath {|fileName|
		^this.class.filenameSymbol.asString.dirname +/+ "assets" +/+ fileName;
	}

	*prLoadJsonFile {|fileName|
		^JSONlib.parseFile(TestJSONlib.prJsonFilePath(fileName));
	}

	// encoding tests
	test_objectEncode {
		var o = (\foo: (\bar: "baz"));
		var j = JSONlib.dumps(o);
		this.assertEquals(j, "{ \"foo\": { \"bar\": \"baz\" } }");
	}

	test_objectStringKeysEncode {
		var o = ("foo": "bar");
		var j = JSONlib.dumps(o);
		this.assertEquals(j, "{ \"foo\": \"bar\" }", "use strings as keys");
	}

	test_arrayEncode {
		var o = [20, 30, 40];
		var j = JSONlib.dumps(o);
		this.assertEquals(j, "[ 20, 30, 40 ]");
	}

	test_valuesEncode {
		var o = (
			\string: "string",
			\number: 10,
			\object: (\foo: "bar"),
			\array: [1,2,3],
			\true: true,
			\false: false,
			\null: JSONlibNull,
		);
		var j = JSONlib.dumps(o);
		this.assertEquals(j, "{ \"array\": [ 1, 2, 3 ], \"false\": false, \"null\": \"JSONlibNull\", \"number\": 10, \"object\": { \"foo\": \"bar\" }, \"string\": \"string\", \"true\": true }");
	}

	test_stringsEncode {
		var o = (
			\text: "lorem ipsum",
			\quotationMark: "lorem\"ipsum",
			\reverseSolidus: "lorem\\ipsum",
			\solidus: "lorem/ipsum",
			\backspace: "lorem%ipsum".format(0x08.asAscii),
			\formfeed: "lorem%ipsum".format(0x0c.asAscii),
			\linefeed: "lorem\nipsum",
			\carriageReturn: "lorem\ripsum",
			\horizontalTab: "lorem\tipsum",
			// @todo non ascii chars
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem ipsum")),
			"{ \"text\": \"lorem ipsum\" }",
			"normal text"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem\"ipsum")),
			"{ \"text\": \"lorem\\\"ipsum\" }",
			"quatition mark"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem\\ipsum")),
			"{ \"text\": \"lorem\\\\ipsum\" }",
			"reverse solidus"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem/ipsum")),
			"{ \"text\": \"lorem\\/ipsum\" }",
			"solidus"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem%ipsum".format(0x08.asAscii))),
			"{ \"text\": \"lorem\\bipsum\" }",
			"backspace"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem%ipsum".format(0x0c.asAscii))),
			"{ \"text\": \"lorem\\fipsum\" }",
			"formfeed"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem\nipsum")),
			"{ \"text\": \"lorem\\nipsum\" }",
			"linefeed"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem\ripsum")),
			"{ \"text\": \"lorem\\ripsum\" }",
			"carriage return"
		);

		this.assertEquals(
			JSONlib.dumps((\text: "lorem\tipsum")),
			"{ \"text\": \"lorem\\tipsum\" }",
			"horizontal tab"
		);
	}

	test_numberEncode {
		[
			// name, value, representation
			["integer", 10, 10],
			["negative integer", -10, -10],
			["zero", 0, 0],
			["negative zero", -0, 0],
			["float", 3.14, 3.14],
			["negative float", -3.14, -3.14],
			["inf", inf, "\"inf\""],
			["negative inf", -inf, "\"-inf\""],
			["pi", pi, 3.1415926535898],
			["exp writing", 1e5, 100000.0],
			["neg-exp writing", 1e-5, 0.00001],
			["hex", 0x10, 16],
		].do({|v|
			var o = (v[0]: v[1]).postln;
			var j = JSONlib.dumps(o);
			this.assertEquals(
				j,
				"{ \"%\": % }".format(v[0], v[2]),
				"number %".format(v[0])
			);
		});
	}

	test_nullEncode {
		var o = (
			null: JSONlibNull()
		);
		var j = JSONlib.dumps(o);
		this.assertEquals(
			j,
			"{ \"null\": null }",
			"Use JSONlibNull to represent null in event";
		);

		o = Dictionary();
		o.put("null", JSONlibNull());
		j = JSONlib.dumps(o);
		this.assertEquals(
			j,
			"{ \"null\": null }",
			"Use JSONlibNull to represent null in event";
		);

		// .parseJson allows us to store nil in a dict
		// which is not possible otherwise
		o = "{\"null\": null}".parseJSON;
		j = JSONlib.dumps(o);
		this.assertEquals(
			j,
			"{ \"null\": null }",
			"nil should be represented as null"
		);

	}

	// decoding tests - taken from json.org
	// we only test for valid json
	test_objectDecode {
		var j = TestJSONlib.prLoadJsonFile("object.json");
		this.assertEquals(j[\foo][\bar], "baz", "Parse nested objects");
	}

	test_arrayDecode {
		var j = TestJSONlib.prLoadJsonFile("array.json");
		this.assertEquals(j, [20 , 30, 40], "JSON can contain also array as root");
	}

	test_valuesDecode {
		var j = TestJSONlib.prLoadJsonFile("values.json");
		this.assertEquals(j[\string], "string", "Value can be strings");
		this.assertEquals(j[\number], 10, "Value can be integer numebrs");
		this.assertEquals(j[\object][\foo], "bar", "Values can be objects");
		this.assertEquals(j[\array], [1, 2, 3], "Value can be an array");
		this.assertEquals(j[\true], true, "Value can be true");
		this.assertEquals(j[\false], false, "Value can be false");
		this.assertEquals(j[\null].class, JSONlibNull, "Value can be null");
	}

	test_stringsDecode {
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

	test_numbersDecode {
		var j = TestJSONlib.prLoadJsonFile("numbers.json");
		this.assertEquals(j[\integer], 10, "Positive integer");
		this.assertEquals(j[\negativeInteger], -1 * 10, "Negative integer");
		this.assertEquals(j[\float], 10.0, "float");
		this.assertEquals(j[\negativeFloat], -10.0, "negative float");
		this.assertEquals(j[\bigE], 20000000000.0, "Big E writing");
		this.assertEquals(j[\smallE], 20000000000.0, "small e writing");
		this.assertEquals(j[\negativeExponent], 0.0000000002, "negative exponent");
	}

	test_jsonNullDecode {
		var p = TestJSONlib.prJsonFilePath("values.json");
		var j = JSONlib.parseFile(p, toEvent: true);
		this.assertEquals(j[\null].class, JSONlibNull, "As an Event can not store nil as value we implemented JSONlibNull");
		j = JSONlib.parseFile(p, toEvent: false);
		this.assertEquals(j["null"], nil, "the SC dict can store nil");
	}
}
