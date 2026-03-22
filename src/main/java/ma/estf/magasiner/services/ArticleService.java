package ma.estf.magasiner.services;

import ma.estf.magasiner.dao.ArticleDao;
import ma.estf.magasiner.models.dto.ArticleDto;
import ma.estf.magasiner.models.mapper.ArticleMapper;
import java.util.List;
import java.util.stream.Collectors;

public class ArticleService {
    private final ArticleDao dao = new ArticleDao();

    public List<ArticleDto> getAllArticles() {
        return dao.findAll().stream().map(ArticleMapper::toDto).collect(Collectors.toList());
    }

    public void createArticle(ArticleDto dto) {
        dao.save(ArticleMapper.toEntity(dto));
    }

}
