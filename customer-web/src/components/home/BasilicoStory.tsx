import { Container } from "@/components/layout/Container";
import { HomeImageSlot } from "@/components/home/HomeImageSlot";

const STORY_IMAGE_SRC = "/images/restaurant/interior-03.webp";

const storyPrinciples = [
  {
    description: "Simple flavours inspired by the food people love across Italy.",
    number: "01",
    title: "Italian inspiration",
  },
  {
    description:
      "Good ingredients, prepared carefully and allowed to speak for themselves.",
    number: "02",
    title: "Fresh ingredients",
  },
  {
    description:
      "From pizza and garlic bread to lasagna, salads and something sweet.",
    number: "03",
    title: "Pizza and familiar dishes",
  },
  {
    description: "A relaxed place to eat, share food and feel welcome.",
    number: "04",
    title: "Warm Dorchester hospitality",
  },
];

export function BasilicoStory() {
  return (
    <section
      className="home-section home-story"
      id="story"
      aria-labelledby="home-story-title"
    >
      <Container className="home-story__container">
        <div className="home-story__content">
          <p className="type-eyebrow home-section__eyebrow">Our story</p>
          <h2 className="type-h2 home-section__title" id="home-story-title">
            Simple food. Properly made.
          </h2>
          <div className="home-story__copy">
            <p className="type-body">
              Basilico is our take on relaxed Italian dining in Dorchester —
              generous sourdough pizza, comforting favourites and warm
              hospitality. We keep things straightforward: good ingredients,
              familiar flavours and food made with care.
            </p>
            <p className="type-body">
              Whether you are joining us at the table, collecting dinner or
              ordering to enjoy at home, the idea is simple — serve food people
              genuinely want to come back for.
            </p>
          </div>
        </div>

        <div className="home-story__editorial">
          <HomeImageSlot
            alt="Basilico wood-fired oven with flame inside the restaurant"
            className="home-story__media"
            imageClassName="home-story__image"
            sizes="(max-width: 767px) 92vw, 30vw"
            src={STORY_IMAGE_SRC}
          />

          <ul className="home-story__notes" aria-label="Basilico values">
            {storyPrinciples.map((principle) => (
              <li className="home-story__note" key={principle.number}>
                <span className="home-story__note-number">{principle.number}</span>
                <span className="home-story__note-content">
                  <span className="home-story__note-title">{principle.title}</span>
                  <span className="home-story__note-description">
                    {principle.description}
                  </span>
                </span>
              </li>
            ))}
          </ul>
        </div>
      </Container>
    </section>
  );
}
