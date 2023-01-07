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

	*parse {|string, convertDicts=true, postWarnings=true|
		if(string.isKindOf(String).not, {
			"Can only parse a String to JSON but received %".format(string.class).warn;
			^();
		});
		^JSONlib.prConvertToSC(string.parseJSON, convertDicts, postWarnings);
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

	*prConvertToSC { |v, convertDicts, postWarnings|
		var res;
		^case
		{ v.isString and: { v.every { |x| x.isDecDigit } } } { v.asInteger }
		// see https://www.json.org/json-en.html Number section and
		// https://stackoverflow.com/a/6425559/3475778
		{ v.isString and: "^[-]?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?$".matchRegexp(v.asString) } { v.asFloat }
		{ v == "true" } { true }
		{ v == "false" } { false }
		// an event can not store nil as a value so we replace it with JSONNull
		{ v == nil } { if(convertDicts, {JSONlibNull()}, {nil}) }
		{ v.isArray } { v.collect { |x| JSONlib.prConvertToSC(x, convertDicts, postWarnings) } }
		{ v.isKindOf(Dictionary) } {
			if(convertDicts) {
				res = Event.new;
				v.pairsDo { |key, x|
					res.put(key.asSymbol, JSONlib.prConvertToSC(x, convertDicts, postWarnings));
				};
				res;
			} {
				v.collect { |x| JSONlib.prConvertToSC(x, convertDicts, postWarnings) }
			}
		}
		{ v }
	}
}