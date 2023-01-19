JSONlib {

	var <>postWarnings;
	var <>useEvent;
	var <>customEncoder;
	var <>customDecoder;

	*new { |postWarnings = true, useEvent=true, customEncoder=nil, customDecoder=nil|
		^super.newCopyArgs(
			postWarnings,
			useEvent,
			customEncoder,
			customDecoder,
		);
	}

	*convertToJSON {|object, customEncoder=nil, postWarnings=true|
		if((object.isKindOf(Dictionary) or: (object.isKindOf(SequenceableCollection))).not) {
			Error("Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class)).throw
		};
		^this.new(postWarnings, customEncoder: customEncoder).prConvertToJson(object);
	}

	*convertToSC {|string, customDecoder=nil, useEvent=true, postWarnings=true|
		if(string.isKindOf(String).not) {
			Error("Can only parse a String to JSON but received %".format(string.class)).throw
		};
		^this.new(
			postWarnings,
			customDecoder: customDecoder,
			useEvent: useEvent
		).prConvertToSC(string.parseJSON)
	}

	*parseFile {|filePath, customDecoder=nil, useEvent=true, postWarnings=true|
		^this.new(
			postWarnings,
			customDecoder: customDecoder,
			useEvent: useEvent,
		).prConvertToSC(filePath.parseJSONFile);
	}

	prConvertToJson {|v|
		var array;
		if(customEncoder.notNil) {
			var val = customEncoder.value(v);
			if(val.notNil) {
				^val;
			};
		};
		^case
		{ v.isKindOf(Symbol) } { this.prConvertToJson(v.asString) }
		// only check value if it is a ref
		{ v.isNil or: { v.isKindOf(Ref) and: { v.value.isNil } } } { "null" }
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
		if(customDecoder.notNil) {
			var val = customDecoder.value(v);
			if(val.notNil) {
				^val;
			};
		};
		^case
		{ v.isString and: { v.every { |x| x.isDecDigit } } } { v.asInteger }
		// see https://www.json.org/json-en.html Number section and
		// https://stackoverflow.com/a/6425559/3475778
		{ v.isString and: { "^[-]?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?$".matchRegexp(v.asString) } } { v.asFloat }
		{ v == "true" } { true }
		{ v == "false" } { false }
		// an event can not store nil as a value so wrap it in a Ref
		{ v == nil } { Ref(nil) }
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
