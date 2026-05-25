import { getElements } from "./dom.js";
import { UiController } from "./ui-controller.js";
import { EncoderClient } from "./encoder-client.js";
import { AppController } from "./app-controller.js";
import { SampleTextService } from "./sample-text-service.js";
import { SampleController } from "./sample-controller.js";
import { SampleView } from "./sample-view.js";

const elements = getElements();
const ui = new UiController(elements);
const sampleView = new SampleView(elements);
const encoderClient = new EncoderClient();
const sampleTextService = new SampleTextService();
let appController = null;
const sampleController = new SampleController({
  elements,
  ui,
  sampleTextService,
  sampleView,
  getCurrentMode: () => appController?.getCurrentMode() ?? "flash",
});
appController = new AppController({
  elements,
  ui,
  encoderClient,
  sampleController,
  sampleView,
});

appController.initialize();
