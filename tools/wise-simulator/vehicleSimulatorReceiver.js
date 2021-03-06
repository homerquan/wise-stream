// a test consumer for vehicle simulator
var amqp = require('amqplib');
var q = 'wise.inbound.event_queue';
var ex = 'wise.inbound.event_queue';

// Consumer
amqp.connect('amqp://localhost').then(function(conn) {
	process.once('SIGINT', function() {
		conn.close();
	});
	return conn.createChannel().then(function(ch) {
		var ok = ch.assertExchange(ex, 'fanout', {
			durable: true
		});
		ok = ok.then(function() {
			return ch.assertQueue(q, {
				exclusive: false // make sure the queue will not be deleted after disconnection
			});
		});
		ok = ok.then(function(qok) {
			return ch.bindQueue(qok.queue, ex, '').then(function() {
				console.log("[Queue] " + qok.queue);
				return qok.queue;
			});
		});
		ok = ok.then(function(queue) {
			return ch.consume(queue, logMessage, {
				noAck: true
			});
		});
		return ok.then(function() {
			console.log(' [*] Waiting for logs. To exit press CTRL+C');
		});

		function logMessage(msg) {
			console.log(" [x] '%s'", msg.content.toString());
		}
	});
}).then(null, console.warn);