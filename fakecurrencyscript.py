from flask import Flask, request, jsonify
import numpy as np
import pandas as pd
import seaborn as sns
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from scipy.stats import kurtosis, skew, entropy
import cv2
import base64
import io
from PIL import Image

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB

@app.route('/api/upload', methods=['POST'])
def upload():
    try:
        if request.method == 'POST':
            if 'image' not in request.files:
                return jsonify({"error": "No image provided"}), 400
            
            image_file = request.files['image']
            
            # Read image data and decode it
            image_data = image_file.read()
            image_data = np.frombuffer(image_data, np.uint8)
            image = cv2.imdecode(image_data, cv2.IMREAD_COLOR)
            
            # Load data and preprocess
            data = pd.read_csv('banknote_authentication.txt', header=None)
            data.columns = ['var', 'skew', 'curt', 'entr', 'auth']

            sns.pairplot(data, hue='auth')
            sns.countplot(x=data['auth'])
            target_count = data.auth.value_counts()

            nb_to_delete = target_count[0] - target_count[1]
            data = data.sample(frac=1, random_state=42).sort_values(by='auth')
            data = data[nb_to_delete:]

            x = data.loc[:, data.columns != 'auth']
            y = data.loc[:, data.columns == 'auth']
            x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.3, random_state=42)

            scalar = StandardScaler()
            scalar.fit(x_train)
            x_train = scalar.transform(x_train)
            x_test = scalar.transform(x_test)

            clf = LogisticRegression(solver='lbfgs', random_state=42, multi_class='auto')
            clf.fit(x_train, y_train.values.ravel())

            def preprocess_image(image):
                norm_image = cv2.normalize(image, None, alpha=0, beta=1, norm_type=cv2.NORM_MINMAX, dtype=cv2.CV_32F)
                img_blur = cv2.GaussianBlur(norm_image, (3,3), 0)
                return img_blur
            
            preprocessed_image = preprocess_image(image)
            
            var = np.var(preprocessed_image, axis=None)
            sk = skew(preprocessed_image, axis=None)
            kur = kurtosis(preprocessed_image, axis=None)
            ent = entropy(preprocessed_image, axis=None) / 100

            result = clf.predict([[var, sk, kur, ent]])

            out = "Real Currency" if result[0] == 0 else "Fake Currency"

            return jsonify({'result': out}), 200
            
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
