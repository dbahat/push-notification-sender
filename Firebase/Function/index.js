const functions = require('firebase-functions');
// The Firebase Admin SDK to access the Firebase Realtime Database. 
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

// Create and Deploy Your First Cloud Functions
// https://firebase.google.com/docs/functions/write-firebase-functions

exports.sendPush = functions.https.onRequest((req, res) => {
  const tokenId = req.get('Authorization').split('Bearer ')[1];

  return admin.auth().verifyIdToken(tokenId)
    .then(function(decoded) { 
      console.log("Successfully authenticated");
      if (decoded.uid !== 'EGjatSWgFPXM8Y3uzYTtVm2NEv22') {
        console.log("Got unauthorized user access for uid: " + decoded.uid)
        res.status(403).send("unauthorized");
        return;
      }
      if (req.body.topic === undefined) {
        console.log("Got a message without a topic. body:" + JSON.stringify(req.body))
        res.status(400).send('No topic defined!');
        return;
      } 
      
      var payload = {
        notification: {
          body: req.body.body
        }
      };
      
      if (req.body.title) {
        payload.notification.title = req.body.title
      }

      admin.messaging().sendToTopic(req.body.topic, payload)
      .then(function(response) {
        console.log("Successfully sent message:", response);
        res.status(200).send()
      })
      .catch(function(error) {
        console.log("Error sending message:", error);
        res.status(500).send(error)
      });
    })
    .catch((err) => res.status(401).send(err));
  });
