+ Collection {
	asJSON {|customEncoder=nil, postWarnings=true|
		^JSONlib.convertToJSON(this, customEncoder, postWarnings);
	}
}

+ Dictionary {
	asJSON {|customEncoder=nil, postWarnings=true|
		^JSONlib.convertToJSON(this, customEncoder, postWarnings);
	}
}
