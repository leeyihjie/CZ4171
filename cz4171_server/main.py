import flask
import io
import string
import time
import os
import numpy as np
import tensorflow as tf
from PIL import Image
from flask import Flask, jsonify, request
from keras.preprocessing import image
from keras.applications.resnet import ResNet50
from keras.applications.resnet import preprocess_input, decode_predictions

model = ResNet50(weights='imagenet')  # Pre-trained model


def imageProcessing(img):
    img = Image.open(io.BytesIO(img))
    img = img.resize((224, 224))
    img = np.array(img)
    img = np.expand_dims(img, 0)
    img = preprocess_input(img)
    return img


app = Flask(__name__)

classes = ['cat', 'dog']

@app.route('/predict', methods=['GET' ,'POST'])
def imagePrediction():
    if 'file' not in request.files:
        print("No file found")
        return jsonify(Error="Please try again. This Image doesn't exist")

    file = request.files.get('file')

    if not file:
        return jsonify(Error="Not file")

    img_bytes = file.read()
    img = imageProcessing(img_bytes)
    predictions = model.predict(img)

    raw_predictions = decode_predictions(predictions, top=3)[0]
    object_names = []
    for i in raw_predictions:
        object_names.append(i[1])
    print('Predicted:', object_names)
    return jsonify(prediction=','.join(object_names))


@app.route('/', methods=['GET'])
def index():
    return 'Deep Learning Model Inference'


if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5001, debug=True)
