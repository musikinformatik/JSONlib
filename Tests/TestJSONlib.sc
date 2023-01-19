TestJSONlib : UnitTest {

	// util
	getPathFor {|fileName|
		^this.class.filenameSymbol.asString.dirname.dirname +/+ "assets" +/+ fileName
	}

	parseJsonFile {|fileName|
		^JSONlib.parseFile(this.getPathFor(fileName))
	}

	// encoding tests
	test_objectEncode {
		var o = (\foo: (\bar: "baz"));
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(j, "{ \"foo\": { \"bar\": \"baz\" } }");
	}

	test_objectStringKeysEncode {
		var o = ("foo": "bar");
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(j, "{ \"foo\": \"bar\" }", "use strings as keys");
	}

	test_arrayEncode {
		var o = [20, 30, 40];
		var j = JSONlib.convertToJSON(o);
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
			\null: `nil,
		);
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(j, "{ \"array\": [ 1, 2, 3 ], \"false\": false, \"null\": null, \"number\": 10, \"object\": { \"foo\": \"bar\" }, \"string\": \"string\", \"true\": true }");
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
			JSONlib.convertToJSON((\text: "lorem ipsum")),
			"{ \"text\": \"lorem ipsum\" }",
			"normal text"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem\"ipsum")),
			"{ \"text\": \"lorem\\\"ipsum\" }",
			"quatition mark"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem\\ipsum")),
			"{ \"text\": \"lorem\\\\ipsum\" }",
			"reverse solidus"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem/ipsum")),
			"{ \"text\": \"lorem\\/ipsum\" }",
			"solidus"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem%ipsum".format(0x08.asAscii))),
			"{ \"text\": \"lorem\\bipsum\" }",
			"backspace"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem%ipsum".format(0x0c.asAscii))),
			"{ \"text\": \"lorem\\fipsum\" }",
			"formfeed"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem\nipsum")),
			"{ \"text\": \"lorem\\nipsum\" }",
			"linefeed"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem\ripsum")),
			"{ \"text\": \"lorem\\ripsum\" }",
			"carriage return"
		);

		this.assertEquals(
			JSONlib.convertToJSON((\text: "lorem\tipsum")),
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
			var j = JSONlib.convertToJSON(o);
			this.assertEquals(
				j,
				"{ \"%\": % }".format(v[0], v[2]),
				"number %".format(v[0])
			);
		});
	}

	test_nullEncode {
		var o = (
			null: `nil
		);
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(
			j,
			"{ \"null\": null }",
			"Use Ref(nil) to represent null in event";
		);

		// .parseJson allows us to store nil in a dict
		// which is not possible otherwise
		o = "{\"null\": null}".parseJSON;
		j = JSONlib.convertToJSON(o);
		this.assertEquals(
			j,
			"{ \"null\": null }",
			"nil should be represented as null"
		);
	}

	// invalid encoding tests
	test_numberAsKey {
		var o = (
			1: 2
		);
		// should print a warning (how to test this?)
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(
			j,
			"{ \"1\": 2 }",
			"numbers as keys will be implicitly converted to strings"
		);
	}

	// encoding of non-json values
	test_functionAsValue {
		var o = (
			\func: {|x| "hello".postln}
		);
		// should print a warning (how to test this?)
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(
			j,
			// needs escaping within function
			"{ \"func\": \"{|x| \\\"hello\\\".postln}\" }",
			"Use compile string as fallback for non-JSON objects which needs escaping",
		);
	}

	test_functionsAsValueRecursive {
		// same as above but now the json of a json as a value
		// primarly used to validate escaping of strings
		var o = (
			\func: {"hello".postln},
		);
		var j = JSONlib.convertToJSON((\json: JSONlib.convertToJSON(o)));
		this.assertEquals(
			j,
			// sorry...
			"{ \"json\": \"{ \\\"func\\\": \\\"{\\\\\\\"hello\\\\\\\".postln}\\\" }\" }",
			"when used recursively we must escape all strings properly"
		);
	}

	// additional ref/nil test
	test_anotherFunctionAsValue {
		// primary purpose to verify null check
		var o = (
			\func: { |x| x !? (x+1) },
		);
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(
			j,
			"{ \"func\": \"{ |x| x !? (x+1) }\" }",
			".value should not get called on a non-ref",
		);
	}

	test_ref {
		var o = (
			\ref: `42,
		);
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(
			j,
			"{ \"ref\": \"`(42)\" }",
			"Non-nil refs should be encoded as compile strings",
		);
	}

	test_stringNull {
		var o = (
			\null: "null",
		);
		var j = JSONlib.convertToJSON(o);
		this.assertEquals(
			j,
			"{ \"null\": \"null\" }",
			"A \"null\" string should be represented as a string",
		)
	}

	// decoding tests - taken from json.org
	// we only test for valid json
	test_objectDecode {
		var j = this.parseJsonFile("object.json");
		this.assertEquals(j[\foo][\bar], "baz", "Parse nested objects");
	}

	test_arrayDecode {
		var j = this.parseJsonFile("array.json");
		this.assertEquals(j, [20 , 30, 40], "JSON can contain also array as root");
	}

	test_valuesDecode {
		var j = this.parseJsonFile("values.json");
		this.assertEquals(j[\string], "string", "Value can be strings");
		this.assertEquals(j[\number], 10, "Value can be integer numbers");
		this.assertEquals(j[\object][\foo], "bar", "Values can be objects");
		this.assertEquals(j[\array], [1, 2, 3], "Value can be an array");
		this.assertEquals(j[\true], true, "Value can be true");
		this.assertEquals(j[\false], false, "Value can be false");
		this.assertEquals(j[\null].class, Ref, "Null needs to be wrapped in a Ref");
		this.assertEquals(j[\null].value, nil, "Unwrapping Ref(nil) should be nil");
	}

	test_stringsDecode {
		var j = this.parseJsonFile("strings.json");
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
			j[\hexDigits].ascii[0..4].collect(_.asAscii),
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
			j[\hexDigits].ascii[8..].collect(_.asAscii),
			"ipsum",
			"Hex encoding should not affect ascii chars ipsum",
		);
	}

	test_numbersDecode {
		var j = this.parseJsonFile("numbers.json");
		this.assertEquals(j[\integer], 10, "Positive integer");
		this.assertEquals(j[\negativeInteger], -1 * 10, "Negative integer");
		this.assertEquals(j[\float], 10.0, "float");
		this.assertEquals(j[\negativeFloat], -10.0, "negative float");
		this.assertEquals(j[\bigE], 20000000000.0, "Big E writing");
		this.assertEquals(j[\smallE], 20000000000.0, "small e writing");
		this.assertEquals(j[\negativeExponent], 0.0000000002, "negative exponent");
	}

	test_jsonNullDecode {
		var p = this.getPathFor("values.json");
		var j = JSONlib.parseFile(p, useEvent: true);
		this.assert(j.keys.asArray.includes(\null), "Null ref value needs to have a key in event");
		this.assertEquals(j[\null].value, nil, "As an Event can not store nil we wrap it in a Ref");
		j = JSONlib.parseFile(p, useEvent: false);
		j.keys.asArray.postln;
		this.assert(j.keys.asArray.any({|x| x == "null"}), "Null ref value needs to have a string key in dictionary");
		this.assertEquals(j["null"].value, nil, "As a Dictionary can not store nil we wrap it in a Ref");
	}

	test_jsonEncodeDict {
		var t = "{ \"hello\": \"world\" }";
		var j = JSONlib.convertToSC(t, useEvent: false);
		this.assertEquals(j.class, Dictionary, "Type should be Dictionary if not Event");
		this.assertEquals(j["hello"], "world", "Dictonaries use Strings as keys");
		this.assertEquals(j[\hello], nil, "Dictionaries use Strings as keys");
	}

	test_jsonEncodeEvent {
		var t = "{ \"hello\": \"world\" }";
		var j = JSONlib.convertToSC(t, useEvent: true);
		this.assertEquals(j.class, Event, "Type should be Event as default");
		this.assertEquals(j["hello"], nil, "Events use symbols as keys");
		this.assertEquals(j[\hello], "world", "Events use symbols as keys");
	}

	// test custom decoder/encoders
	test_customEncoderFunc {
		var f = {|x|
			if(x.isFunction) {
				"--func%".format(x.cs).quote;
			}
		};
		var o = (
			\f: {|x| x},
		);
		var j = JSONlib.convertToJSON(
			object: o,
			customEncoder: f
		);
		this.assertEquals(
			j,
			"{ \"f\": \"--func{|x| x}\" }",
			"Custom encoder values should be used for translating values to JSON",
		);
		this.assertEquals(
			JSONlib.convertToJSON((\o: 42), f),
			"{ \"o\": 42 }",
			"Custom encoder should not affect other values",
		);
	}

	test_customDecoderFunc {
		var f = {|x|
			if(x.isString) {
				if(x.beginsWith("--func")) {
					// somehow we need to run this twice
					x["--func".size()..].compile().();
				};
			};
		};
		var t = "{ \"f\": \"--func{|x| 42;}\" }";
		var o = JSONlib.convertToSC(
			t,
			customDecoder: f
		);
		o.postln;
		this.assertEquals(
			o[\f].(),
			42,
			"Custom decoder should be used for translating JSON values to SC objects"
		);
		this.assertEquals(
			JSONlib.convertToSC("{ \"f\": 20 }", f)[\f],
			20,
			"Custom encoder should not affect other values",
		);
	}

	// test external methods
	test_arrayMethod {
		var j = [0, nil, "20"].asJSON();
		this.assertEquals(
			j,
			"[ 0, null, \"20\" ]",
			"Test array method"
		)
	}

	test_dictMethod {
		var d = Dictionary().put("some", 20);
		var j = d.asJSON();
		this.assertEquals(
			j,
			"{ \"some\": 20 }",
			"Test external method on a Dictionary"
		);
	}

	test_eventMethod {
		var e = (\some: 30);
		var j = e.asJSON();
		this.assertEquals(
			j,
			"{ \"some\": 30 }",
			"Test external method on an Event"
		);
	}
}
