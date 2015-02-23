var csv = require('ya-csv');
var _ = require('underscore');
var q = 'wise.inbound.event_queue';
var ex = 'wise.inbound.event_queue';
var mqConn = require('amqplib').connect('amqp://localhost');

var reader = csv.createCsvFileReader('resources/truckPoints.csv', {
	columnsFromHeader: true
});

// Publisher
mqConn.then(function(conn) {
	var ok = conn.createChannel();
	ok = ok.then(function(ch) {
		var okEx = ch.assertExchange(ex, 'fanout', {
			durable: false
		});
		okEx.then(function() {
			reader.addListener('data', function(data) {
				// supposing there are so named columns in the source file
				ch.publish(ex, '', new Buffer(JSON.stringify(data)));
				// slow down 10s for simulator
				// TODO: using exchange queue to control speed
				reader.pause();
				_.delay(reader.resume, 10000);
			});
		});
	});
	return ok;
}).then(null, console.warn);