import flask
import io
import string
import time
import os
import numpy as np
import tensorflow as tf
import werkzeug
from PIL import Image, ImageOps
from flask import Flask, jsonify, request, make_response
from keras.preprocessing import image
from keras.applications.resnet import ResNet50
from keras.applications.resnet import preprocess_input, decode_predictions
import ssl

model = ResNet50(weights='imagenet')  # Pre-trained model using ResNet50 and imagenet weights


# Function to pre-process image
def prepare_image(picture):
    picture = Image.open(picture)
    picture = picture.resize((224, 224))
    picture = np.array(picture)
    picture = np.expand_dims(picture, 0)
    picture = preprocess_input(picture)
    return picture


app = Flask(__name__)


@app.route('/predict', methods=['POST'])
def predict_image():
    if 'file' not in request.files:
        return jsonify(Error="Image not found. Please try again!")
    if request.method == "POST":
        # Get image file from request sent from android app
        image_file = request.files.get('file')
        image_filename = werkzeug.utils.secure_filename(image_file.filename)
        print("\nReceived image File name : " + image_file.filename)
        image_file.save(image_filename)  # Save uploaded image to server
        # Prepare image with aformentioned preprocessing steps
        img = prepare_image(image_file)
        print("Image Pre-processing done!")
        predictions = model.predict(img)
        print("Class of image predicted!")

        ssl._create_default_https_context = ssl._create_unverified_context  # To bypass HTTPS SSL error faced.

        top_3_predictions_raw = decode_predictions(predictions, top=3)[0]
        print("Top 3 Raw Predictions: ", top_3_predictions_raw)

        top_1_prediction_raw = decode_predictions(predictions, top=1)[0]
        print("Top 1 Raw Prediction: ", top_1_prediction_raw)
        prediction_list = []
        for i in top_1_prediction_raw:
            prediction_list.append(i[1])
        print('Predicted:', prediction_list)
        # return jsonify(Error="Image not found, please try again!")  # Testing
        return jsonify(Prediction=','.join(prediction_list))


# Front Page of server
@app.route('/', methods=['GET'])
def index():
    return 'Prediction Application for CZ4171 Course Project'


if __name__ == '__main__':
    # pred1 = predict("cat1.jpeg")
    # print(pred1)
    app.run(host="0.0.0.0", port=5001, debug=True)
